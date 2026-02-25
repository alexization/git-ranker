# Request Spec Template

Use this template for feature, bugfix, refactor, infra, workflow, and docs requests.

## 0) Metadata
- Type: `feature | bugfix | refactor | infra | workflow | docs`
- Title:
- Owner:
- Priority: `P0 | P1 | P2 | P3`
- Related issue:
- Related plan path:

## 1) Goal
- Problem:
- Intended outcome:
- Non-goals:

## 2) Scope
- In scope:
- Out of scope:

## 3) Constraints
- Architecture constraints:
- Security and PII constraints:
- Performance/SLO constraints:
- Compatibility constraints:
- Documentation update requirements:

## 4) Acceptance Criteria
1. 
2. 
3. 

## 5) Validation Plan
- Commands:
  - `<unit-test-command>`
  - `<integration-test-command>` (if needed)
- Runtime checks:
  - logs:
  - metrics:
  - health:
- Example mapping:
  - Spring Boot: `<unit-test-command>` -> `./gradlew test`
  - Spring Boot: `<integration-test-command>` -> `./gradlew integrationTest`

## 6) Risks and Rollback
- Risks:
- Rollback strategy:

## 7) Clarification Log (Korean Questions)
- Q1:
- A1:
- Q2:
- A2:

## 8) Spec Status
- Status: `Draft | Clarifying | Approved`
- Approved by:
- Approval timestamp:

---

## Type-Specific Additions

### Feature
- API contract:
  - endpoint:
  - request schema:
  - response schema:
- Data impact:
  - schema/migration/backfill:

### Bugfix
- Reproduction steps:
- Root cause hypothesis:
- Regression test to add:

### Refactor
- Invariants that must not change:
- Behavior compatibility checks:
- Performance baseline and target:

### Infra
- Environment/secrets impact:
- Deployment/rollback sequence:
- Monitoring/alert impact:

### Workflow
- Affected workflow or automation:
- Trigger and guardrail impact:
- Rollback sequence:

### Docs
- Source of truth sections updated:
- Any behavior/policy impact:
- Cross-link updates:
