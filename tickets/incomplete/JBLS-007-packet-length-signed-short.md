---
id: JBLS-007
title: BNLS packet length is read into a signed short
status: open
type: bug
severity: low
component: BNLSConnectionThread
created: 2026-10-04
found_by: code reading (untested)
---

## Summary
`short pLength` (`BNLSConnectionThread.java:159`) holds a WORD length from the wire. Values ≥ 0x8000 go negative, so `while (bytesRead < pLength - 3)` reads nothing and the stream desyncs. Lengths < 3 behave the same way.

## Failure scenario
A client sends a header with length 0xFFFF. JBLS reads no body, then treats the client's following bytes as new packet headers.

## Notes
Real BNLS packets are small, so this only matters for malformed or hostile input.
