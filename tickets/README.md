# Tickets

Flat-file bug tickets, one markdown file per ticket.

- `incomplete/`: open work
- `complete/`: done; move the file here when it's resolved

Filename: `JBLS-NNN-short-slug.md`

## Frontmatter

```yaml
---
id: JBLS-NNN
title: One-line summary
status: needs live verification case
type: bug | limitation | task
severity: high | medium | low
component: BNLSParse | ConnectionThread | SRP | CheckRevision | build | ...
created: 2026-10-04
found_by: code reading (untested) | live test | ...
---
```

## Statuses in use

- `open`: the defect is clear from the code; it needs a fix
- `needs live verification case`: the code suggests a problem (or a behavior to check), but it has to be confirmed against a real client or server
- `needs design`: it's a limitation, not a defect; the approach needs deciding

## Body sections

Summary, Location, Details, Failure scenario, Verification, Notes. Leave out any section that doesn't apply.
