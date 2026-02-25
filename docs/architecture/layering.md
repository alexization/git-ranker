# Layering Rules

## Purpose
Define dependency boundaries so humans and AI agents can modify code safely.

## Package Responsibilities
- `com.gitranker.api.domain`
  - Entities, value objects, repositories, use-case services, controllers.
- `com.gitranker.api.infrastructure`
  - GitHub API clients, mappers, token pools, external adapters.
- `com.gitranker.api.global`
  - Shared config, security, auth, error handling, response wrappers, logging.
- `com.gitranker.api.batch`
  - Batch jobs, schedulers, tasklets, processors, listeners, batch metrics.

## Dependency Direction
1. `domain` -> `global` is allowed for shared concerns (`ApiResponse`, `BusinessException`).
2. `domain` must not depend directly on infrastructure implementation details.
3. `infrastructure` must not contain core business policy.
4. `batch` can depend on `domain` and `infrastructure` orchestration points.
5. `global` should remain framework/cross-cutting and avoid domain behavior.

## Change Checklist
1. Does this change cross package boundaries?
2. If yes, is the dependency direction still valid?
3. If design intent changed, add/update ADR.
4. Add tests that fail if behavior regresses.

## Guardrail Roadmap
- Phase 4 target: add ArchUnit tests to enforce boundaries in CI.
