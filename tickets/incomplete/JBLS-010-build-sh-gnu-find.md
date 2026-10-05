---
id: JBLS-010
title: build.sh uses GNU-only `find -name` and fails on macOS
status: open
type: bug
severity: low
component: build
created: 2026-10-04
found_by: build attempt 2026-10-04 (macOS, BSD find)
---

## Summary
`build.sh:5` runs `find -name '*.java'` with no starting path. BSD find (macOS) requires a path, so the script fails under `set -e`.

## Notes
Fix: `find . -name '*.java'`. The commented alternative on line 11 has the same problem.

On 2026-10-04 it was built manually with `javac @sources` + `jar cfm` on Homebrew OpenJDK 21.0.10 (via jenv), producing `JBLS-b942f2f.jar`. The compile gave 3 deprecation warnings (`new Integer(int)`, marked for removal).
