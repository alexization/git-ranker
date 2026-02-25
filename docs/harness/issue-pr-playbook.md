# Issue/Commit/PR Playbook

Use this playbook to execute harness phases with consistent granularity.

## Branch Naming
- `harness/m1-1-agents-docs-index`
- `harness/m1-2-layering-test-runbook`
- `harness/m1-3-template-upgrade`
- `harness/m2-1-observability-runbook`
- `harness/m2-2-metrics-catalog`
- `harness/m2-3-logging-contract`
- `harness/m3-1-plan-workflow`
- `harness/m3-2-adr-workflow`
- `harness/m3-3-doc-migration`
- `harness/m4-1-pr-guardrail`
- `harness/m4-2-archunit-guardrail`
- `harness/m4-3-weekly-scorecard`
- `harness/m5-1-coverage-quality-gate`
- `harness/m5-2-deploy-health-hardening`
- `harness/m5-3-pii-safe-logging-defaults`
- `harness/m5-4-spec-gate-policy`

## Commit Rule
1. One commit = one intent.
2. Prefix format:
- `docs(...)`
- `chore(...)`
- `ci(...)`
- `test(...)`
- `refactor(...)`
3. Commit message title should be written in Korean.
4. Keep 2 to 4 commits per PR.

## PR Rule
1. One PR per harness task issue.
2. PR body should be written in Korean.
3. PR must include:
- linked issue
- plan path
- validation evidence
- risk and rollback
4. Practical mode only: move related plan from `docs/plans/active` to `docs/plans/completed` in the same PR before final merge.

## Spec Gate Rule (Required Before Execution)
1. Apply spec gate for feature, bugfix, refactor, and infra changes.
2. Use [spec-gate.md](spec-gate.md) and [request-spec-template.md](request-spec-template.md).
3. Required fields must be fully filled before implementation.
4. Clarification questions to the user must be written in Korean.
5. Start implementation only after explicit spec approval.
6. If new unknowns appear mid-implementation, pause and request spec change approval first.

## Execution Order
1. Spec gate approval
2. M1-1 -> M1-2 -> M1-3
3. M2-1 -> M2-2 -> M2-3
4. M3-1 -> M3-2 -> M3-3
5. M4-1 -> M4-2 -> M4-3
6. M5-1 -> M5-2 -> M5-3 -> M5-4

## Ready-to-Create Issue Titles
1. `[Harness Epic]: M1 Increase Application Legibility`
2. `[Harness Task]: M1-1 Add AGENTS entrypoint and docs index`
3. `[Harness Task]: M1-2 Define layering and testing runbook`
4. `[Harness Task]: M1-3 Upgrade issue/pr templates for intent spec`
5. `[Harness Epic]: M2 Full Observability Stack for Agent Work`
6. `[Harness Task]: M2-1 Add local observability runbook`
7. `[Harness Task]: M2-2 Add metrics catalog and SLI targets`
8. `[Harness Task]: M2-3 Standardize structured logging contract`
9. `[Harness Epic]: M3 Establish Docs as System of Record`
10. `[Harness Task]: M3-1 Add execution plan lifecycle`
11. `[Harness Task]: M3-2 Add ADR workflow`
12. `[Harness Task]: M3-3 Backfill recent critical decisions`
13. `[Harness Epic]: M4 Build Continuous Feedback Loop`
14. `[Harness Task]: M4-1 Enforce PR contract in CI`
15. `[Harness Task]: M4-2 Add architecture guardrail tests`
16. `[Harness Task]: M4-3 Add weekly scorecard and drift review`
17. `[Harness Epic]: M5 Raise Verification Confidence`
18. `[Harness Task]: M5-1 Add coverage quality gate`
19. `[Harness Task]: M5-2 Harden deployment health checks`
20. `[Harness Task]: M5-3 Enforce PII-safe logging defaults`
21. `[Harness Task]: M5-4 Enforce spec approval gate and Korean clarification loop`
