# Plan: 2026-02-25-m4-2-architecture-guardrail

## 1) Purpose
- Problem: architecture constraints were documented but not automatically enforced.
- Intended outcome: add ArchUnit guardrails to fail CI when critical dependency boundaries are violated.
- Non-goals: full package redesign or strict clean architecture migration.

## 2) Scope
- In scope:
  - add ArchUnit test dependency
  - add initial architecture guardrail tests
  - update architecture docs with enforced rules
- Out of scope:
  - enforce all documented boundaries in one step
  - refactor existing layer interactions

## 3) Progress
- [x] add ArchUnit dependency to `build.gradle`
- [x] add initial guardrail test class
- [x] update architecture docs with active enforcement scope

## 4) Design Notes
- Constraints: rules must pass current codebase while still preventing high-risk regressions.
- Tradeoffs: start with a minimal/high-signal rule set to avoid immediate false-positive noise.
- Open questions: when to tighten domain-to-infrastructure constraints.

## 5) Decision Log
- 2026-02-25: enforce three batch-boundary prohibitions (`domain/global/infrastructure` must not depend on `batch`).
- 2026-02-25: enforce location rules for `@RestController` and `@RestControllerAdvice`.

## 6) Validation Plan
- Required commands:
  - `./gradlew test --tests "com.gitranker.api.architecture.ArchitectureGuardrailTest"`
- Observability checks:
  - not applicable (architecture test change)

## 7) Risks and Rollback
- Risks: rule strictness may block legitimate cross-layer scenarios.
- Rollback strategy: relax or split specific failing rules while preserving high-signal constraints.

## 8) Result Summary
- What changed: ArchUnit-based architecture guardrails were added to the test suite.
- Test result summary:
  - `./gradlew test --tests "com.gitranker.api.architecture.ArchitectureGuardrailTest"` passed
- Follow-up tasks:
  - incrementally add stricter boundary rules with migration plans
