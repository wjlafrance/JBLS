---
id: JBLS-003
title: Server-side NLS logon checking (0x12/0x13/0x14) is not functional
status: open
type: bug
severity: medium
component: BNLSParse / SRP
created: 2026-10-04
found_by: code reading (untested)
---

## Summary
The source comments mark `BNLS_RESERVESERVERSLOTS`, `BNLS_SERVERLOGONCHALLENGE` and `BNLS_SERVERLOGONPROOF` as "Fully Supported", but they can't produce a correct server-side SRP exchange.

## Location / details
1. **Slot counter never set.** `private int SRPs = 0;` (`BNLSParse.java:48`) is never assigned. The checks `if (SRPs < slot) return null;` (`:943`, `:991`) mean any slot > 0 gets no response.
2. **B isn't reduced mod N.** `SRP.get_B(v)` returns `v + g^b` (`Hashing/SRP.java:111`) without `% N`. The 0x13 reply then copies only the first 32 bytes.
3. **Wrong math for the server side.** `onServerLogonChallenge` builds `new SRP(A)` with an empty username and password. The salt and v from the account database are read but never stored on the slot. `onServerLogonProof` calls `getM2(get_A(), get_B())`, passing A as the salt and using the client-side formulas, which depend on the password. The server can't compute S = (A·v^u)^b mod N this way.
4. **Proof reply echoes the client.** 0x14 sends `reply.addDWord(M1[Y])` for Y=0..4: the first 5 *bytes* of the client's M1, each widened to a DWORD, instead of the server's 20-byte proof.

Carl Bennett's commit `b05aba1` (2019-03-07, "Fix incorrect BNLS_SERVERLOGONCHALLENGE/PROOF responses") fixed the B byte width and a stray `if`, but not the items above.

## Failure scenario
A BNCS server delegates WAR3 account logon to JBLS: 0x12 (reserve 1 slot), 0x13 (slot 0), 0x14 (slot 0). The success flag is effectively random (M1 compared to a wrong M2), and the client rejects the server proof.

## Verification
Unit test against a known-good SRP vector (for example from bncsutil, or captured from a real logon).

## Notes
Only matters if Atlas delegates NLS to BNLS. The client-side NLS path (0x02/0x03/0x0A) is separate; see JBLS-012.
