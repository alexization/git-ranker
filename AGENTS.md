# AGENTS.md

Operating guide for humans and AI agents in `git-ranker`.

## 1) Mission
- Build and maintain a reliable Spring Boot backend for Git Ranker.
- Optimize for repeatable delivery, not one-off heroics.
- Treat repository documents as executable operating policy.

## 2) First 5 Minutes Checklist
1. Read [docs/index.md](docs/index.md).
2. Confirm task scope and linked issue.
3. For non-trivial changes, create a plan from [docs/plans/TEMPLATE.md](docs/plans/TEMPLATE.md).
4. Pick the smallest safe implementation slice.
5. Decide verification commands before coding.

## 3) Mandatory Delivery Flow (Issue -> Plan -> PR)
1. Issue
- Use `.github/ISSUE_TEMPLATE` forms.
- Define intent, scope, and success criteria.

2. Plan
- Create `docs/plans/active/YYYY-MM-DD-<slug>.md`.
- Track progress and decisions in the same file.

3. Implementation
- Keep commits small and narratable.
- Respect package boundaries and existing patterns.

4. Validation
- Run targeted commands first, then broader checks.
- Record exact commands and outcomes in PR.

5. Documentation
- Update runbook/architecture/ADR when behavior or design changes.
- Move finished plans to `docs/plans/completed/`.

## 4) Canonical Commands
- Build: `./gradlew build -x test`
- Unit test: `./gradlew test`
- Integration test: `./gradlew integrationTest`
- Single unit class: `./gradlew test --tests "com.gitranker.api.domain.user.service.UserRegistrationServiceTest"`
- Single integration class: `./gradlew integrationTest --tests "com.gitranker.api.domain.user.UserRepositoryIT"`
- Local run: `./gradlew bootRun`

## 5) Layer Boundaries
- `domain/*`
  - Owns business rules and use-case orchestration.
  - Must not depend on `infrastructure/*` internals.
- `infrastructure/*`
  - Adapters for GitHub API, token pools, external clients.
  - Must not hold core business policy.
- `global/*`
  - Cross-cutting concerns only (auth, config, error, response, logging).
- `batch/*`
  - Scheduled/batch pipelines and listeners.
  - Keep batch-specific logic out of controllers.

## 6) Error, Response, and Transaction Rules
- Use `BusinessException(ErrorType.XXX)` for domain failures.
- Keep exception mapping in `GlobalExceptionHandler`.
- Return `ApiResponse.success(...)` / `ApiResponse.error(...)`.
- Place transaction boundaries in service layer.

## 7) Observability Contract
- When adding/changing endpoint, verify:
  - health endpoint behavior
  - metrics visibility (`/actuator/prometheus`)
  - structured logs include key identifiers (never secrets)
- When changing batch/integration logic, verify:
  - failure path logging
  - metrics changes (counters/timers) if behavior changed

## 8) Definition of Done
- Linked issue exists.
- Plan file exists for non-trivial changes.
- Validation commands and outcomes recorded.
- Docs updated or explicitly marked not needed.
- Risks and rollback path documented in PR.

## 8.1) Language Convention
- Commit message titles should be written in Korean.
- PR descriptions should be written in Korean.
- Keep code identifiers and commands in original syntax.

## 9) System of Record
- Index: [docs/index.md](docs/index.md)
- Harness roadmap: [docs/harness/roadmap.md](docs/harness/roadmap.md)
- Issue/PR playbook: [docs/harness/issue-pr-playbook.md](docs/harness/issue-pr-playbook.md)
- Architecture: [docs/architecture/layering.md](docs/architecture/layering.md)
- Testing runbook: [docs/runbooks/testing.md](docs/runbooks/testing.md)
- Observability runbook: [docs/runbooks/observability-local.md](docs/runbooks/observability-local.md)
