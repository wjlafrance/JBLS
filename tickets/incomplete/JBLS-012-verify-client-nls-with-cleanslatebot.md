---
id: JBLS-012
title: Verify client-side NLS login flow by running CleanSlateBot against JBLS
status: needs live verification case
type: task
severity: medium
component: BNLSParse / SRP
created: 2026-10-04
found_by: planned live test
---

## Summary
Confirm end to end that JBLS's client-side NLS path produces a working WAR3 logon, by running CleanSlateBot.ocx through JBLS.

## Messages under test
CSB's WAR3 (`Product = "3RAW"`) path, per `cleanslatebot-research/README.md`:
- 0x0E `BNLS_AUTHORIZE` → 0x0F `BNLS_AUTHORIZEPROOF` (on connect)
- 0x0D `BNLS_CHOOSENLSREVISION`
- 0x02 `BNLS_LOGONCHALLENGE` → BNCS `SID_AUTH_ACCOUNTLOGON` (0x53)
- 0x03 `BNLS_LOGONPROOF` → BNCS `SID_AUTH_ACCOUNTLOGONPROOF` (0x54)
- (optional) 0x0A `BNLS_CONFIRMLOGON` to check the server proof
- plus whichever CheckRevision and CD-key messages CSB uses (not yet identified)

## Prerequisites
- **A BNCS server that accepts NLS logon.** Atlas has no handlers for 0x52–0x58, so WAR3/W3XP can't log in today (see `bnet-dev/docs/PRD.md`).
- **CSB's BNLS host is hardcoded** to `www.valhallalegends.com:9367`, so it has to be redirected to JBLS (hosts file or binary patch).
- CSB2 needs `Accept = 579728`.
- WAR3 hash files in JBLS `IX86/WAR3/` matching the version the BNCS server expects.

## Pass criteria
CSB reaches chat on WAR3 through JBLS, and the BNCS server accepts its M1 proof.

## Notes
Related: JBLS-001 (version check mapping), JBLS-009 (0x0F reply format).
