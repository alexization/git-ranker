# Plan: 2026-02-25-m5-4-spec-gate-policy

## 1) Purpose
- Problem: implementation can start with under-specified requests, causing rework and inconsistent outcomes.
- Intended outcome: enforce a pre-implementation spec gate and provide a reusable request template.
- Non-goals: domain logic changes, runtime behavior changes, or infrastructure replacement.

## 2) Scope
- In scope:
  - add spec gate policy under `docs/harness`
  - add request spec template optimized for feature/bugfix/refactor/infra tasks
  - update `AGENTS.md` and `docs/index.md` references
  - require Korean clarification questions during spec completion
- Out of scope:
  - changing GitHub issue form schemas
  - adding new CI jobs for spec linting

## 3) Progress
- [x] move completed M5-1 plan from `active` to `completed`
- [x] add `spec-gate.md` policy
- [x] add `request-spec-template.md`
- [x] update `AGENTS.md`, docs index, and harness playbook/roadmap
- [x] create issue, commit, and PR linked to this plan (`#69`, `#70`)

## 4) Design Notes
- Constraints: keep machine-read policy docs in English.
- Tradeoffs: enforce strict readiness before coding; accept slight upfront communication overhead.
- Open questions: whether to automate spec field checks in CI in a later phase.

## 5) Decision Log
- 2026-02-25: use explicit `Approved` gate status before implementation.
- 2026-02-25: clarification questions to users must be asked in Korean.

## 6) Validation Plan
- Required checks:
  - verify cross-links in `AGENTS.md` and `docs/index.md`
  - verify `docs/harness` includes policy + template
  - verify M5-1 plan file is under `docs/plans/completed`

## 7) Risks and Rollback
- Risks:
  - process may feel heavier for trivial tasks
- Rollback strategy:
  - relax gate from strict-required to recommended in `spec-gate.md`
  - keep template but remove blocking language in `AGENTS.md`

## 8) Result Summary
- What changed:
  - added spec gate policy and request template
  - linked policy in AGENTS/docs index/playbook/roadmap
  - moved completed M5-1 plan from active to completed
- Validation summary:
  - documentation cross-links and plan locations verified
- Follow-up tasks:
  - move this plan to `docs/plans/completed/` after PR merge
