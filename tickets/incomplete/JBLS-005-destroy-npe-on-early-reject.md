---
id: JBLS-005
title: Destroy() throws NullPointerException on the IP-ban and max-threads rejection paths
status: open
type: bug
severity: low
component: BNLSConnectionThread
created: 2026-10-04
found_by: code reading (untested)
---

## Summary
The max-threads check (`BNLSConnectionThread.java:~113-117`) and the IP-auth check (`:~124-128`) call `Destroy()` before `out` and `in` are assigned. `Destroy()` calls `out.close()` (`:87`) on `null`, which throws `NullPointerException`. Only `IOException` is caught, so the socket is never closed.

## Failure scenario
A banned IP connects (with `IPAuth` on), or connection 501 arrives. The thread throws NPE and the client socket leaks until the peer closes it.

## Verification
Ban 127.0.0.1 in the IP auth list, connect, and check for an NPE in stderr.

## Notes
Root cause is Java's nullable-by-default reference types. `out` and `in` are instance fields that stay `null` until `run()` assigns them, and the `OutputStream` type doesn't express that, so the compiler accepts `out.close()`. (`Optional<T>` and `@Nullable` are conventions only; javac doesn't enforce them.) Fix: null-guard in `Destroy()`, or close the `socket` directly, since that closes its streams.
