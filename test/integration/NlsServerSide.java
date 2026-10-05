package integration;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Test-side, independent implementation of Battle.net's half of the WarCraft III
 * NLS (SRP) logon, revision 2. Written from the published algorithm, not from JBLS's SRP class,
 * so it can check JBLS's client-side answers.
 *
 * Wire values (salt, A, B, S) are 32-byte little-endian.
 */
final class NlsServerSide {
  // N as an integer (big-endian hex). The 32 wire bytes of N are this, little-endian.
  static final BigInteger N = new BigInteger("F8FF1A8B619918032186B68CA092B5557E976C78C73212D91216F6658523C787", 16);
  static final BigInteger G = BigInteger.valueOf(47);

  private final String user;   // upper-cased
  private final byte[] salt;
  private final BigInteger v;
  private final BigInteger b;
  final byte[] B;

  NlsServerSide(String username, String password, byte[] salt, byte[] bPrivate) {
    this.user = username.toUpperCase();
    this.salt = salt;
    BigInteger x = le(sha1(salt, sha1((user + ":" + password.toUpperCase()).getBytes(StandardCharsets.US_ASCII))));
    this.v = G.modPow(x, N);
    this.b = le(bPrivate);
    this.B = toLe32(v.add(G.modPow(b, N)).mod(N));
  }

  /** M1 the client should send, given its A. */
  byte[] expectedM1(byte[] A) {
    byte[] K = K(A);
    return sha1(I(), sha1(user.getBytes(StandardCharsets.US_ASCII)), salt, A, B, K);
  }

  /** Server proof M2 = H(A, M1, K). */
  byte[] M2(byte[] A, byte[] M1) {
    return sha1(A, M1, K(A));
  }

  private byte[] K(byte[] A) {
    // u = first 4 bytes of H(B), as a big-endian integer
    byte[] hB = sha1(B);
    BigInteger u = new BigInteger(1, new byte[] { hB[0], hB[1], hB[2], hB[3] });
    // S = (A * v^u)^b mod N
    BigInteger S = le(A).multiply(v.modPow(u, N)).modPow(b, N);
    byte[] s = toLe32(S);
    byte[] even = new byte[16], odd = new byte[16];
    for (int i = 0; i < 16; i++) { even[i] = s[i * 2]; odd[i] = s[i * 2 + 1]; }
    byte[] h1 = sha1(even), h2 = sha1(odd);
    byte[] K = new byte[40];
    for (int i = 0; i < 20; i++) { K[i * 2] = h1[i]; K[i * 2 + 1] = h2[i]; }
    return K;
  }

  /** I = H(N) xor H(g), over their little-endian wire bytes. */
  static byte[] I() {
    byte[] hN = sha1(toLe32(N));
    byte[] hG = sha1(new byte[] { 47 });
    byte[] I = new byte[20];
    for (int i = 0; i < 20; i++) I[i] = (byte) (hN[i] ^ hG[i]);
    return I;
  }

  static BigInteger le(byte[] littleEndian) {
    byte[] be = new byte[littleEndian.length];
    for (int i = 0; i < be.length; i++) be[i] = littleEndian[littleEndian.length - 1 - i];
    return new BigInteger(1, be);
  }

  static byte[] toLe32(BigInteger n) {
    byte[] be = n.toByteArray();
    byte[] out = new byte[32];
    for (int i = 0; i < 32 && i < be.length; i++) out[i] = be[be.length - 1 - i];
    return out;
  }

  static byte[] sha1(byte[]... parts) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      for (byte[] p : parts) md.update(p);
      return md.digest();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  static String hex(byte[] b) {
    return HexFormat.of().formatHex(b);
  }
}
