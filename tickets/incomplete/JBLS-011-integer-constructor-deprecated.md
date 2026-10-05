---
id: JBLS-011
title: new Integer(int) is deprecated for removal
status: open
type: task
severity: low
component: BNLSParse / util
created: 2026-10-04
found_by: javac 21.0.10 warnings
---

## Summary
javac 21 warns `[removal] Integer(int) in Integer has been deprecated and marked for removal` at:
- `BNLSProtocol/BNLSParse.java:769`
- `BNLSProtocol/BNLSParse.java:770`
- `util/PEFiles/ImportAddressManager.java:53`

It will stop compiling on a future JDK.

## Notes
Replace with `Integer.valueOf(...)` or autoboxing.
