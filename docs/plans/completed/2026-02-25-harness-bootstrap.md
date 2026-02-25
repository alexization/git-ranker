# Plan: 2026-02-25-harness-bootstrap

## 1) Purpose
- Problem: repository-level agent operating rules were implicit and scattered.
- Intended outcome: establish issue-to-plan-to-pr workflow with enforceable PR contract.
- Non-goals: introduce new runtime business features.

## 2) Scope
- In scope:
  - root `AGENTS.md`
  - `docs` system-of-record structure
  - issue/pr templates for harness workflow
  - CI guardrail for PR body contract
- Out of scope:
  - ArchUnit enforcement
  - production observability architecture changes

## 3) Progress
- [x] Add AGENTS and docs index
- [x] Add plans/adr/runbook structure
- [x] Add harness issue forms and PR template contract
- [x] Add CI guardrail for PR body checks

## 4) Design Notes
- Constraints: keep existing build/test split and avoid disruptive runtime changes.
- Tradeoffs: started with lightweight CI text contract before deeper static guardrails.
- Open questions: strictness level for docs-only PR exceptions.

## 5) Decision Log
- 2026-02-25: enforce plan link in PR body to prevent undocumented work.
- 2026-02-25: add roadmap doc mapping issue, commit, and PR per phase.

## 6) Validation Plan
- Required commands:
  - no runtime code changes; command execution deferred
- Observability checks:
  - not applicable for docs/process bootstrap

## 7) Risks and Rollback
- Risks: PR guardrail may be too strict for small docs changes.
- Rollback strategy: relax regex checks in `.github/workflows/harness-pr-guardrail.yml`.

## 8) Result Summary
- What changed: operating model scaffolding and enforcement entrypoints were added.
- Test result summary: not run (docs/process-only changes).
- Follow-up tasks:
  - add architecture tests (ArchUnit)
  - add metrics catalog and SLI document
