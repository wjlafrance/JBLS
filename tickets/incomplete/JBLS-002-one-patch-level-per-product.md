---
id: JBLS-002
title: Only one hash-file set and verbyte per product; can't serve multiple patch levels
status: needs design
type: limitation
severity: medium
component: CheckRevision / Constants / settings.ini
created: 2026-10-04
found_by: code reading
---

## Summary
Each product has exactly one `HashPath` (`IX86/<PROD>/`), one `Exe`/`Storm`/`Network` file set, one `VerByte` and one `Version`. A single JBLS instance can only answer version checks for one patch level per product.

## Location
- `util/Constants.java`: `IX86files`, `IX86verbytes`, `IX86versions`
- `settings.ini`: `[<PROD>-IX86]` sections
- CheckRevision V1–V4 caches (`crCache`) are keyed by `formula + mpq + prod + plat`. The cache key doesn't include the file set.

## Details
The Battle.net Archival Project PRD (`bnet-dev/docs/PRD.md`) calls for pinning a product to any patch level. Pinning server-side means the BNLS has to produce CheckRevision results for whichever game version is pinned.

## Verification
None needed; this is a structural limitation.

## Notes
Open design question: per-version hash folders chosen by verbyte, by a config switch, or by running one JBLS instance per pinned level.
