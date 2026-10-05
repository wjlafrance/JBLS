---
id: JBLS-004
title: Uncaught RuntimeException in a handler kills the thread and leaks socket and thread count
status: open
type: bug
severity: high
component: BNLSConnectionThread / BNLSParse
created: 2026-10-04
found_by: code reading (untested)
---

## Summary
`parseInput` catches only `IndexOutOfBoundsException` (`BNLSParse.java:152`). The connection loop catches only `InvalidPacketException`, `InterruptedIOException`, `IOException` and `BNLSException`. Any other `RuntimeException` ends `run()` without closing the socket, without `threadCount--` and without `Destroy()`.

## Location
- `BNLSProtocol/BNLSConnectionThread.java` `run()`, around lines 150–226
- `BNLSProtocol/BNLSParse.java:152`

## Failure scenario
Send `BNLS_SERVERLOGONCHALLENGE` (0x13) without sending 0x12 first. `reservedSRPs` is `null`, so `reservedSRPs[slot] = ...` throws `NullPointerException`. The thread dies, the socket stays open and `threadCount` stays raised. After `MaxThreads` (500) such events, every new connection is refused with "Max Threads Exceeded".

## Verification
Send 0x13 as the first packet, then check JBLS logs and `lsof` for the open socket. Repeat to watch `threadCount` climb.

## Notes
Fix idea: a catch-all `catch (RuntimeException e)` in the loop, plus cleanup in `finally`.
