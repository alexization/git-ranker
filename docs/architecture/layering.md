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
- Phase 4 active: ArchUnit tests now enforce baseline rules in CI.

## Enforced Rules (Current)
1. Classes in `..domain..` must not depend on `..batch..`.
2. Classes in `..infrastructure..` must not depend on `..batch..`.
3. Classes in `..global..` must not depend on `..batch..`.
4. `@RestController` classes must reside in `..domain..`.
5. `@RestControllerAdvice` classes must reside in `..global..`.

## Next Tightening Candidates
1. Add stricter domain-to-infrastructure dependency constraints.
2. Split broad packages into smaller bounded contexts before stronger rules.
