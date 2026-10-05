---
id: JBLS-013
title: SRP modulus N is a static field shared by all connections; null until set_NLS()
status: open
type: bug
severity: medium
component: SRP
created: 2026-10-04
found_by: code reading, while converting SRP.main to a JUnit test
---

## Summary
`private static BigIntegerEx N = null;` (`Hashing/SRP.java`) is set by the instance method `set_NLS(revision)` to N1 (rev 1) or N2 (rev 2). Because it's static, every SRP instance on every connection thread shares one modulus.

## Details
- Each `BNLSParse` handler calls `mySRP.set_NLS(nlsRevision)` right before using SRP, and that write is visible to all threads.
- Before any call to `set_NLS()`, `N` is `null`, so `get_v`/`get_A`/`get_S` throw `NullPointerException`. The old `SRP.main` hit this: it never called `set_NLS()`, so it couldn't run.

## Failure scenario
Bot 1 (NLS rev 1) and bot 2 (NLS rev 2, via 0x0D) log on at the same time. Bot 2's `set_NLS(2)` lands between bot 1's `set_NLS(1)` and its `get_A()`/`getM1()`. Bot 1's A or M1 is then computed with N2, and its logon fails at random.

## Verification
A test that runs two SRP instances with different revisions on concurrent threads and checks the results against `SRPTest`'s single-threaded values.

## Notes
Fix: make `N` an instance field.
