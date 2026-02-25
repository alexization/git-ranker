# Plan: 2026-02-25-m2-2-metrics-catalog

## 1) Purpose
- Problem: metrics are scattered across code and dashboards, making operational criteria hard to evaluate quickly.
- Intended outcome: establish a stable metrics catalog and SLI targets for Git-Ranker.
- Non-goals: metric collection code refactoring, alert policy redesign.

## 2) Scope
- In scope:
  - `docs/observability/metrics-catalog.md`
  - link consistency across `docs/index.md`, `AGENTS.md`, and runbooks
- Out of scope:
  - adding new metric instrumentation code
  - dashboard structure redesign

## 3) Progress
- [x] collect metric inventory from code and dashboard queries
- [x] define initial SLI targets
- [x] write catalog document and link from index files

## 4) Design Notes
- Constraints: document only metrics that already exist in production code/dashboard.
- Tradeoffs: initial SLI thresholds are conservative and should be tuned with real traffic history.
- Open questions: long-term validity of the GitHub API success target (98%).

## 5) Decision Log
- 2026-02-25: start with five SLI axes: availability, error rate, latency, GitHub API success rate, batch success rate.
- 2026-02-25: annotate metric sources at the code-class level in the catalog.

## 6) Validation Plan
- Required commands:
  - docs-only change, runtime command not required
- Observability checks:
  - validate metric names against code (`BusinessMetrics`, `BatchMetrics`, `GitHubApiMetrics`) and dashboard queries

## 7) Risks and Rollback
- Risks: some SLI thresholds may not match real traffic behavior.
- Rollback strategy: adjust thresholds in documentation only (no code rollback required).

## 8) Result Summary
- What changed: added M2-2 metrics catalog and SLI reference document.
- Test result summary: docs-only change.
- Follow-up tasks:
  - align structured logging contract documents/code in M2-3
