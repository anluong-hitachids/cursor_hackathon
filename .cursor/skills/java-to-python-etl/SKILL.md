---
name: java-to-python-etl
description: >-
  Phased workflow for migrating Java ETL applications to idiomatic Python with
  behavior and test parity. Walks through: set context, explain source ETL,
  design Python layout, migrate transform logic (pure function), wire CLI,
  port tests, and review. Use when migrating Java ETL/pipelines to Python,
  enforcing cross-language parity, or when the user mentions java-to-python-etl.
---

# Java to Python ETL Migration

## Principles

- Reproduce **behavior and business rules**, not line-by-line syntax.
- Separate **transform** from **I/O** — transforms stay pure and unit-testable.
- Prefer simple, idiomatic Python; avoid unnecessary frameworks.

## Workflow

Execute phases **in order**. Read the linked step file when entering each phase.

```
Progress:
- [ ] Phase 0  Context        → 00_context.md
- [ ] Phase 1  Explain        → 01_explain.md
- [ ] Phase 2  Design         → 02_design.md
- [ ] Phase 3  Migrate logic  → 03_migrate_logic.md
- [ ] Phase 4  CLI app        → 04_cli_app.md
- [ ] Phase 5  Tests          → 05_tests.md
- [ ] Phase 6  Review         → 06_review.md
```

### Phase 0 — Context

Read [00_context.md](00_context.md). Adopt the role, goals, and constraints before touching any code.

### Phase 1 — Explain

Read [01_explain.md](01_explain.md). Document the Java ETL's extract, transform, and load steps as bullet points. Ignore Java syntax; focus on **what** the code does.

### Phase 2 — Design

Read [02_design.md](02_design.md). Propose a Python module layout that separates transform logic from file I/O and is easy to unit-test.

### Phase 3 — Migrate logic

Read [03_migrate_logic.md](03_migrate_logic.md). Implement a pure `transform(lines: list[str])` function with no file I/O. Preserve every validation rule from the Java source.

### Phase 4 — CLI app

Read [04_cli_app.md](04_cli_app.md). Write a thin CLI entrypoint that reads input, calls `transform()`, writes output, and prints a summary.

### Phase 5 — Tests

Read [05_tests.md](05_tests.md). Port the Java unit tests to pytest (or unittest). Preserve every scenario and assertion; do not add extra logic.

### Phase 6 — Review

Read [06_review.md](06_review.md). Verify behavior parity, check edge cases, and suggest readability improvements.

## Quality gate

Before treating work as done:

1. Confirm **behavior parity** — same output for the same input as the Java app.
2. Confirm **test parity** — every Java test scenario is covered.
3. Run the tests and the CLI to verify end-to-end.
