---
id: JBLS-009
title: BNLS_AUTHORIZEPROOF always returns STATUS_AUTHORIZED and appends 4 non-spec bytes
status: needs live verification case
type: bug
severity: low
component: BNLSParse
created: 2026-10-04
found_by: code reading (untested)
---

## Summary
`onAuthorizeProof` (`BNLSParse.java:780-818`):
- Returns status `0x00` (authorized) for anonymous logins too. The spec text in the same file says anonymous logins get `0x01` (STATUS_UNAUTHORIZED).
- Appends `connection.address.getAddress()` (4 bytes, the client IP) after the status DWORD. The spec doesn't include this.

## Verification
Live: CleanSlateBot sends 0x0E then 0x0F on connect (`cleanslatebot-research/README.md`). Confirm CSB accepts the 0x0F reply as-is and goes on to its next BNLS request.

## Notes
*(Inference)* The extra 4 bytes may be a later BNLS extension that JBLS copied, rather than a JBLS invention. Not checked.
