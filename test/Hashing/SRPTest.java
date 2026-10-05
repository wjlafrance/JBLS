package Hashing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HexFormat;

import org.junit.jupiter.api.Test;

import util.BigIntegerEx;

/**
 * Characterization test for client-side SRP (formerly SRP.main).
 *
 * Expected values were captured from JBLS's own output on 2026-10-04
 * (Davnit/JBLS develop @ b942f2f, OpenJDK 21.0.10). They pin current
 * behavior; they have not been checked against a real Battle.net logon.
 */
public class SRPTest {
  private static final HexFormat HEX = HexFormat.of();

  private static final String USERNAME = "TestUser";
  private static final String PASSWORD = "hunter2";
  private static final byte[] SALT = HEX.parseHex("3f4de10bbf41cf3fca9f8774e5baecfd641d9151f8075654b8942daa1f0bc154");
  private static final byte[] B    = HEX.parseHex("89117fa8d7f66fb948945d2f698229f5e713131df84b99ba8d9686aabb261918");
  private static final byte[] A_PRIVATE = HEX.parseHex("62bf76862df193aa909c94e49fa7ba38f5ad14d7d9cd2d891b3bebf7202685a4");

  private static final String X = "7654c214560f600f5d0d47a8be73da5050d006d3000000000000000000000000";
  private static final String U = "6f6d440100000000000000000000000000000000000000000000000000000000";

  private static SRP newSRP(int nlsRevision) {
    SRP srp = new SRP(USERNAME, PASSWORD, new BigIntegerEx(BigIntegerEx.LITTLE_ENDIAN, A_PRIVATE));
    srp.set_NLS(nlsRevision);
    return srp;
  }

  @Test
  public void nlsRevision1() {
    SRP srp = newSRP(1);
    assertEquals(X, HEX.formatHex(srp.get_x(SALT).toByteArray()), "x");
    assertEquals("29d6550eda965a9e4873e99af810bc832ae8c509d11e75bd411cafd56df36204", HEX.formatHex(srp.get_v(SALT).toByteArray()), "v");
    assertEquals("d9f8b6868c1a01fd0944d051117cc5f5217ccb46793feabc20a9f991d9c9713b", HEX.formatHex(srp.get_A()), "A");
    assertEquals(U, HEX.formatHex(srp.get_u(B).toByteArray()), "u");
    assertEquals("d0942d3d1dd2bb48051a0324fb7d98e3fd593995a70c26d7eb0b100d05c3ad32", HEX.formatHex(srp.get_S(SALT, B)), "S");
    assertEquals("e921b4b83736f905b638bb3650a602155b059c01815b8ba22b176399dfc2bb471fe1129a55f63e8e", HEX.formatHex(srp.get_K(srp.get_S(SALT, B))), "K");
    assertEquals("add1e1463090c190a825175451e45789d74ebf20", HEX.formatHex(srp.getM1(SALT, B)), "M1");
    assertEquals("22d2c136ec725f6d3b4ed31f093f6b50127201b2", HEX.formatHex(srp.getM2(SALT, B)), "M2");
  }

  @Test
  public void nlsRevision2() {
    SRP srp = newSRP(2);
    assertEquals(X, HEX.formatHex(srp.get_x(SALT).toByteArray()), "x");
    assertEquals("667b0a4ffbbd123f8de44e30347425f81c41a81c4c8a64e844b4d546a1c6560b", HEX.formatHex(srp.get_v(SALT).toByteArray()), "v");
    assertEquals("498f541c527a7138a63b9bef2194225634632e3f0868d4b0c46595db90caa574", HEX.formatHex(srp.get_A()), "A");
    assertEquals(U, HEX.formatHex(srp.get_u(B).toByteArray()), "u");
    assertEquals("6207e0c4754bf8b263cfb95836139379142ee1c8596b2e63151010cd2e60fb87", HEX.formatHex(srp.get_S(SALT, B)), "S");
    assertEquals("935d5db3e26958bf14f16862299fddf5a32f8e81ca04262a5e72df60e43bcda9cf75bd162b8684c0", HEX.formatHex(srp.get_K(srp.get_S(SALT, B))), "K");
    assertEquals("071c191641c71c3c215b52c16664a8a2ec69f2ba", HEX.formatHex(srp.getM1(SALT, B)), "M1");
    assertEquals("a75c795821af1c3092ea6e0b66006ea9aaf2bb0a", HEX.formatHex(srp.getM2(SALT, B)), "M2");
  }
}
