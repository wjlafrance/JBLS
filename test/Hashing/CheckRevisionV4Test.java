package Hashing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import util.Constants;
import util.Out;

/**
 * Characterization test for CheckRevisionV4 (formerly CheckRevisionV4.main).
 *
 * Expected values were captured from JBLS's own output on 2026-10-04
 * (Davnit/JBLS develop @ b942f2f, OpenJDK 21.0.10). They pin current
 * behavior; they have not been checked against a live Battle.net challenge.
 */
public class CheckRevisionV4Test {
  private static final int D2DV = Constants.PRODUCT_DIABLO2;
  private static final int DRTL = Constants.PRODUCT_DIABLO;

  // Synthetic certificate value; no real DRTL cert is on hand.
  private static final String SYNTHETIC_DRTL_CERT = "6efc5423bc7ec87dba49717b3f22534026b42683";

  @BeforeAll
  public static void initOutput() {
    // Main does this at startup; without it any Out.* call NPEs.
    Out.setDefaultOutputStream();
  }

  @BeforeEach
  public void resetState() {
    // Results are cached statically by seed + archive + product + platform,
    // not by version or cert, so clear between tests.
    CheckRevisionV4.clearCache();
    Constants.IX86versions[D2DV - 1] = "1.14.3.71";
    Constants.IX86versions[DRTL - 1] = "2001, 5, 18, 1";
    Constants.IX86certs[DRTL - 1] = "";
  }

  private static void assertResult(CheckrevisionResults r, int version, int checksum, String info) {
    assertNotNull(r);
    assertEquals(version, r.getVersion(), "version");
    assertEquals(checksum, r.getChecksum(), "checksum");
    assertEquals(info, new String(r.getInfo().getBuffer(), StandardCharsets.ISO_8859_1), "info");
  }

  @Test
  public void diablo2Seeds() throws IOException {
    // Seeds from the original main()
    assertResult(CheckRevisionV4.checkRevision("0RFf+AAA", D2DV, Constants.PLATFORM_INTEL, "CheckRevision.mpq"),
        0, 0x4f78392b, "qiNZCXbTGzoPvl76Y66lNfg=\0");
    assertResult(CheckRevisionV4.checkRevision("3ou3jQAA", D2DV, Constants.PLATFORM_INTEL, "CheckRevision.mpq"),
        0, 0x706d584d, "g8AEAY1dgVaotHYLUzJgKJo=\0");
    assertResult(CheckRevisionV4.checkRevision("ZYDAHQAA", D2DV, Constants.PLATFORM_INTEL, "CheckRevision.mpq"),
        0, 0x67674c56, "+9kKei3b960lDtOxuQeURoI=\0");
    assertResult(CheckRevisionV4.checkRevision("Ry4VIgAA", D2DV, Constants.PLATFORM_INTEL, "CheckRevision.mpq"),
        0, 0x654c6f61, "Kyl+7a6yKVGWEj7rzhB+Q40=\0");
  }

  @Test
  public void diabloD1MissingCertReturnsNull() throws IOException {
    assertNull(CheckRevisionV4.checkRevision("0RFf+AAA", DRTL, Constants.PLATFORM_INTEL, "CheckRevisionD1.mpq"));
  }

  @Test
  public void diabloD1WithSyntheticCert() throws IOException {
    Constants.IX86certs[DRTL - 1] = SYNTHETIC_DRTL_CERT;
    assertResult(CheckRevisionV4.checkRevision("0RFf+AAA", DRTL, Constants.PLATFORM_INTEL, "CheckRevisionD1.mpq"),
        6, 0x505a6153, "DPlh3qhoqeC++EFlka3nUzU=:/3vmKjbv85kn93CvzVvkfF2jQMQ=\0");
    assertResult(CheckRevisionV4.checkRevision("3ou3jQAA", DRTL, Constants.PLATFORM_INTEL, "CheckRevisionD1.mpq"),
        6, 0x7a5a2b76, "LI0OITCz5rtf1I/AVgsQjz0=:ENTyFNdIsVnq977wNvEDo3rmoLg=\0");
  }
}
