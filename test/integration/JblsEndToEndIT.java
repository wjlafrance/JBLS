package integration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HexFormat;
import java.util.Map;
import java.util.zip.CRC32;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import integration.BnlsClient.Packet;
import integration.BnlsClient.Payload;

/**
 * End-to-end: start JBLS as a process, talk BNLS to it over TCP.
 * Expected values come from independent sources (published test vectors,
 * java.util.zip.CRC32, the test-side NLS server), not from JBLS's own classes.
 */
public class JblsEndToEndIT {
  private static final int BNLS_AUTHORIZE = 0x0E, BNLS_AUTHORIZEPROOF = 0x0F, BNLS_REQUESTVERSIONBYTE = 0x10;
  private static final int BNLS_HASHDATA = 0x0B, BNLS_CHOOSENLSREVISION = 0x0D;
  private static final int BNLS_LOGONCHALLENGE = 0x02, BNLS_LOGONPROOF = 0x03, BNLS_CONFIRMLOGON = 0x0A;
  private static final HexFormat HEX = HexFormat.of();

  private static JblsServer server;

  @BeforeAll
  static void start() throws Exception {
    server = new JblsServer();
  }

  @AfterAll
  static void stop() throws Exception {
    if (server != null) server.close();
  }

  @Test
  void anonymousAuthorize() throws Exception {
    try (BnlsClient c = server.connect()) {
      Packet code = c.request(BNLS_AUTHORIZE, new Payload().ntString("e2e-anon").build());
      assertEquals(BNLS_AUTHORIZE, code.id);
      code.dword(); // server code

      Packet proof = c.request(BNLS_AUTHORIZEPROOF, new Payload().dword(0x12345678).build());
      assertEquals(BNLS_AUTHORIZEPROOF, proof.id);
      assertEquals(0, proof.dword(), "status");
      // JBLS-009: JBLS appends the client IP (not in the BNLS spec). Pinned as current behavior.
      assertArrayEquals(new byte[] { 127, 0, 0, 1 }, proof.bytes(4), "appended client IP");
      assertEquals(0, proof.remaining());
    }
  }

  @Test
  void requestVersionByte() throws Exception {
    // Defaults from util/Constants.IX86verbytes, in BNLS product-ID order 1..11
    int[] expected = { 0xD3, 0xD3, 0x4f, 0x0e, 0x0e, 0xa9, 0x1E, 0x1E, 0x2a, 0x2a, 0xa5 };
    try (BnlsClient c = server.connect()) {
      for (int prod = 1; prod <= 11; prod++) {
        Packet p = c.request(BNLS_REQUESTVERSIONBYTE, new Payload().dword(prod).build());
        assertEquals(prod, p.dword(), "echoed product " + prod);
        assertEquals(expected[prod - 1], p.dword(), "verbyte for product " + prod);
      }
      Packet bad = c.request(BNLS_REQUESTVERSIONBYTE, new Payload().dword(99).build());
      assertEquals(0, bad.dword(), "invalid product");
      assertEquals(0, bad.remaining());
    }
  }

  @Test
  void hashDataMatchesPublishedBrokenSha1Vectors() throws Exception {
    // Vectors from LexManos/JBLS cleanup branch HashTest (92fc307, 2019-07-12)
    String[][] vectors = {
        { "", "67452301efcdab8998badcfe10325476c3d2e1f0" },
        { "a", "fe4424936dc20078a033955159f823036e513f13" },
        { "abc", "7bf57a7acc1be999f975dce65522fe81ab78c8d6" },
    };
    try (BnlsClient c = server.connect()) {
      for (String[] v : vectors) {
        byte[] data = v[0].getBytes();
        Packet p = c.request(BNLS_HASHDATA, new Payload().dword(data.length).dword(0).bytes(data).build());
        StringBuilder got = new StringBuilder();
        for (int i = 0; i < 5; i++) got.append(String.format("%08x", p.dword()));
        assertEquals(v[1], got.toString(), "bsha1(\"" + v[0] + "\")");
      }
      // HASHDATA_FLAG_COOKIE (0x04) echoes the cookie after the hash
      Packet p = c.request(BNLS_HASHDATA, new Payload().dword(3).dword(0x04).bytes("abc".getBytes()).dword(0xCAFEF00D).build());
      p.bytes(20);
      assertEquals(0xCAFEF00D, p.dword(), "cookie");
    }
  }

  @Test
  void warcraft3NlsRevision2LogonAgainstIndependentServer() throws Exception {
    byte[] salt = HEX.parseHex("3f4de10bbf41cf3fca9f8774e5baecfd641d9151f8075654b8942daa1f0bc154");
    byte[] bPrivate = HEX.parseHex("0c81b1bc1d4b8fe1ab3e6a7cd3cc7f0b41b1c4d5c8a0c2e3f4a5b6c7d8e9f0a1");
    NlsServerSide bnet = new NlsServerSide("TestUser", "hunter2", salt, bPrivate);

    try (BnlsClient c = server.connect()) {
      Packet rev = c.request(BNLS_CHOOSENLSREVISION, new Payload().dword(2).build());
      assertEquals(1, rev.dword(), "NLS revision 2 accepted");

      Packet challenge = c.request(BNLS_LOGONCHALLENGE, new Payload().ntString("TestUser").ntString("hunter2").build());
      byte[] A = challenge.bytes(32);
      assertEquals(0, challenge.remaining(), "A is 32 bytes");

      Packet proof = c.request(BNLS_LOGONPROOF, new Payload().bytes(salt).bytes(bnet.B).build());
      byte[] M1 = proof.bytes(20);
      assertEquals(NlsServerSide.hex(bnet.expectedM1(A)), NlsServerSide.hex(M1), "client proof M1");

      Packet ok = c.request(BNLS_CONFIRMLOGON, new Payload().bytes(bnet.M2(A, M1)).build());
      assertEquals(1, ok.dword(), "JBLS accepts the genuine server proof");

      byte[] forged = bnet.M2(A, M1);
      forged[0] ^= 1;
      Packet bad = c.request(BNLS_CONFIRMLOGON, new Payload().bytes(forged).build());
      assertEquals(0, bad.dword(), "JBLS rejects a forged server proof");
    }
  }

  @Test
  void passwordAuthorizeAgainstIndependentCrc32() throws Exception {
    // BNLS checksum: CRC-32 of password + server code as 8 uppercase hex digits.
    // The server code is random per connection; repeat so both high-bit and low-bit CRCs occur.
    String password = "e2e-secret";
    try (JblsServer authServer = new JblsServer(Map.of("RequireAuth", "true"), "",
        Map.of("bots.ini", "[e2e-bot]\nPassword=" + password + "\n"))) {
      int highBit = 0, lowBit = 0;
      for (int i = 0; i < 24; i++) {
        try (BnlsClient c = authServer.connect()) {
          int serverCode = c.request(BNLS_AUTHORIZE, new Payload().ntString("e2e-bot").build()).dword();
          CRC32 crc = new CRC32();
          crc.update((password + String.format("%08X", serverCode)).getBytes());
          int checksum = (int) crc.getValue();
          if (checksum < 0) highBit++; else lowBit++;

          Packet proof = c.request(BNLS_AUTHORIZEPROOF, new Payload().dword(checksum).build());
          assertEquals(0, proof.dword(), "authorized (checksum 0x" + Integer.toHexString(checksum) + ")");
        }
      }
      assertTrue(highBit > 0 && lowBit > 0, "sample covered both CRC sign cases: high=" + highBit + " low=" + lowBit);

      try (BnlsClient c = authServer.connect()) {
        c.request(BNLS_AUTHORIZE, new Payload().ntString("e2e-bot").build());
        c.send(BNLS_AUTHORIZEPROOF, new Payload().dword(0xDEADBEEF).build());
        assertTrue(c.closedByServer(3000), "wrong password checksum disconnects");
      }
    }
  }

  @Test
  void knownBug_JBLS004_serverLogonChallengeBeforeReserveLeaksConnection() throws Exception {
    // Reproduces JBLS-004: 0x13 without a prior 0x12 throws NullPointerException in the
    // connection thread; the socket is left open and the thread count never drops.
    // When JBLS-004 is fixed, flip this to expect a clean disconnect and a freed slot.
    try (JblsServer small = new JblsServer(Map.of("MaxThreads", "2"), "", Map.of())) {
      // awaitListening's probe used one thread slot and released it normally.
      for (int i = 0; i < 2; i++) {
        BnlsClient c = small.connect(); // left open on purpose
        c.send(0x13, new Payload().dword(0).dword(2).bytes(new byte[96]).build());
        assertFalse(c.closedByServer(1000), "socket left open after NPE (JBLS-004)");
      }
      assertTrue(small.output().contains("NullPointerException"), "NPE logged");
      try (BnlsClient c = small.connect()) {
        c.send(BNLS_REQUESTVERSIONBYTE, new Payload().dword(1).build());
        // The reject path also NPEs in Destroy() (JBLS-005), so the socket isn't closed either; it just never answers.
        assertFalse(c.receivesWithin(2000), "slots exhausted: new connection gets no service");
      }
      assertTrue(small.output().contains("Max Threads Exceeded"), "max threads message");
    }
  }
}
