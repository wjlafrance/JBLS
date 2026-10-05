---
id: JBLS-008
title: BNLS_HASHDATA double-hash runs binary data through new String(bytes) (platform charset)
status: needs live verification case
type: bug
severity: low
component: BNLSParse / DoubleHash
created: 2026-10-04
found_by: code reading (untested)
---

## Summary
With `HASHDATA_FLAG_DOUBLEHASH`, `onHashData` calls `DoubleHash.doubleHash(new String(data.getBuffer()), cToken, sToken)` (`BNLSParse.java:533`). Decoding with the default charset (UTF-8 on modern JVMs) changes any byte ≥ 0x80 before it's hashed.

## Failure scenario
An Old Logon System password containing non-ASCII bytes (for example Latin-1 `é` = 0xE9) is hashed as U+FFFD → `EF BF BD`. The bot then sends the wrong password hash.

## Verification
Compare the 0x0B double-hash output with a reference XSHA1 double hash for a password containing 0xE9.

## Notes
Plain (non-double) hashing uses `BrokenSHA1.calcHashBuffer(byte[])` and isn't affected.
