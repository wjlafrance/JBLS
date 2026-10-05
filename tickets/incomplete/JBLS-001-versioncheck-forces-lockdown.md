---
id: JBLS-001
title: BNLS_VERSIONCHECK (0x09) and VERSIONCHECKEX (0x18) always map legacy products to lockdown-IX86-NN.mpq
status: needs live verification case
type: bug
severity: high
component: BNLSParse
created: 2026-10-04
found_by: code reading (untested)
---

## Summary
For STAR, SEXP, W2BN, JSTR, DRTL, DSHR and SSHR, the version-DLL digit sent with 0x09/0x18 is always turned into `lockdown-IX86-0N.mpq`. A BNCS server that issued a pre-Lockdown archive (`IX86verN.mpq` or `ver-IX86-N.mpq`) gets a Lockdown CheckRevision result back, which won't match.

## Location
- `BNLSProtocol/BNLSParse.java:433-441` (`onVersionCheck`)
- `BNLSProtocol/BNLSParse.java:1030-1038` (`onVersionCheckEX`)

## Details
The default `archive = "ver-IX86-" + digit + ".mpq"` is overwritten for the products listed above. `HashMain.getRevision(..., String dll, long filetime)` then picks CheckRevision V3 (Lockdown) from the filename.

0x1A `VERSIONCHECKEX2` isn't affected, because the client sends the real archive filename.

## Failure scenario
1. Atlas sends `ver-IX86-1.mpq` (its hardcoded value) to a STAR client in `SID_AUTH_INFO`.
2. A bot asks JBLS with 0x09 (prod=1, digit=1).
3. JBLS computes Lockdown against `lockdown-IX86-01.mpq`, or fails if the lockdown DLL or `STAR.bin` is missing.
4. The bot sends a wrong checksum in `SID_AUTH_CHECK`.

## Verification
- Live: run CleanSlateBot through JBLS against a BNCS that issues `ver-IX86-N.mpq` or `IX86verN.mpq` for STAR, and capture which BNLS message CSB uses for CheckRevision.
- Unknown: whether CSB uses 0x09, 0x18 or 0x1A. `cleanslatebot-research/README.md` lists the CSB CheckRevision message ID as not identified. *(Inference: CSB was built 2002-12-03, before 0x1A existed, so it's probably 0x09.)*

## Notes
Atlas doesn't currently check CheckRevision results at all (`SID_AUTH_CHECK` stores but never compares them), so this won't show up against stock Atlas until Atlas validates versions.
