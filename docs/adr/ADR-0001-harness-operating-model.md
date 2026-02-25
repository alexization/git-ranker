# ADR-0001: Adopt Harness Operating Model

- Status: Accepted
- Date: 2026-02-25
- Related issue: Harness M1/M3/M4 bootstrap
- Related plan: docs/plans/completed/2026-02-25-harness-bootstrap.md

## Context
- AI agent collaboration requires explicit constraints, documentation routes, and review contracts.
- Existing repository had CI and observability components, but lacked a unified operating entrypoint and enforceable documentation flow.

## Decision
- Adopt `AGENTS.md` as execution entrypoint.
- Adopt `docs/` as system of record with explicit plan/adr/runbook structure.
- Require PR body to include plan path and validation evidence via CI guardrail.

## Consequences
- Positive:
  - clearer onboarding for human and agent contributors
  - improved traceability from issue to implementation evidence
  - reduced undocumented or unverifiable changes
- Negative:
  - additional authoring overhead on each PR
  - strict guardrail may need tuning for edge cases
- Operational impact:
  - contributors must follow issue -> plan -> PR protocol

## Alternatives Considered
1. Keep only README-based guidance without enforcement.
2. Use wiki/external docs as source of truth.
3. Delay enforcement until architecture tests are ready.
