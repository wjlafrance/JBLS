---
id: JBLS-006
title: Global connection list and counters are changed from many threads without synchronization
status: open
type: bug
severity: low
component: BNLSConnectionThread / BNLSServer / Controller
created: 2026-10-04
found_by: code reading (untested)
---

## Summary
- The `Controller.lLinkedHead` doubly-linked list is changed in `BNLSServer` (`:55-61`, on accept) and `BNLSConnectionThread.Destroy()` (`:72-90`, on every connection thread) with no locking.
- `threadCount` and `connectionCount` are plain `static int`. `threadCount` serves as both the thread ID source (`:102`) and the live count. `connectionCount` is never decremented.

## Details
When the head node is removed, `Destroy()` sets `lLinkedHead = bNextList` but doesn't clear `bNextList.prev`. When that node is later destroyed, it takes the `bPrevList != null` branch: it relinks into a dead node and never updates `lLinkedHead`.

## Failure scenario
Two connections close in order A (head) then B (next). After A closes, B still points back at A. When B closes, `lLinkedHead` keeps pointing at B, a closed connection. Anything that walks the list (`BNLSServer:90`) sees stale entries.

## Verification
A stress test with many concurrent connect/disconnect cycles, then walk the list.
