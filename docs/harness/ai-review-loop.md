# AI Review Loop (Codex + CodeRabbitAI)

This document defines the required review loop before merge.

## 1) Goal
- Ensure every PR has two-agent review coverage.
- Apply fixes from findings, then re-run both reviewers.
- Merge only after final review pass and plan lifecycle completion.

## 2) Required Sequence
1. Open PR (recommended as draft).
2. Run Codex review:
   - add PR comment: `@codex review`
3. Run CodeRabbitAI review:
   - if auto review does not run or re-review is needed, add PR comment: `@coderabbitai review`
4. Agent triage:
   - classify findings into `must-fix`, `won't-fix-with-reason`, `false-positive`.
5. Apply required fixes and push commits.
6. Re-run both reviews (`@codex review`, `@coderabbitai review`).
7. Repeat until final findings are resolved or justified.
8. In the same PR, move the linked plan from `docs/plans/active` to `docs/plans/completed`.
9. Update PR body evidence tables and merge-ready checklist.
10. Merge.

## 3) Merge Gate
- Codex final re-review completed.
- CodeRabbitAI final re-review completed.
- Review findings triaged and reflected in code or rationale.
- Plan moved to `docs/plans/completed` in the same PR.

## 4) Evidence Requirement
- PR body must include:
  - Codex review evidence link
  - CodeRabbitAI review evidence link
  - validation command results
  - risk and rollback
