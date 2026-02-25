# Plan: 2026-02-25-m2-3-logging-contract

## 1) Purpose
- Problem: logging fields vary by call site, making automated analysis and correlation difficult.
- Intended outcome: define a structured logging field contract and enforce required fields in code.
- Non-goals: full logging refactor across all call sites, logging infrastructure replacement.

## 2) Scope
- In scope:
  - automate required-field enforcement in `LogContext`
  - align contract fields in key GitHub API/HTTP/Batch logs
  - add logging contract documentation and tests
- Out of scope:
  - full Loki/Grafana dashboard redesign
  - immediate migration of every existing `log.debug` call

## 3) Progress
- [x] define required field contract (`trace_id`, `event`, `log_category`, `phase`, `outcome`)
- [x] implement automatic field guarantees in `LogContext`
- [x] align key GitHub/HTTP/Batch logs with the contract
- [x] add documentation and unit tests for the contract

## 4) Design Notes
- Constraints: preserve existing `Event` and MDC patterns while extending behavior.
- Tradeoffs: default WARN outcome is `warning`, but failure flows explicitly set `outcome=failure`.
- Open questions: migration priority for remaining plain debug logs.

## 5) Decision Log
- 2026-02-25: auto-inject trace/phase/outcome in `LogContext` to remove repetitive call-site code.
- 2026-02-25: explicitly set `outcome=failure` for failed external API WARN logs.

## 6) Validation Plan
- Required commands:
  - `./gradlew test --tests "com.gitranker.api.global.logging.LogContextTest"`
- Observability checks:
  - verify `logging-contract.md` fields match actual code behavior

## 7) Risks and Rollback
- Risks: existing log-parsing queries may not account for new fields.
- Rollback strategy: remove the auto-injection logic in `LogContext` and restore previous behavior.

## 8) Result Summary
- What changed: added logging field contract documentation, code updates, and tests.
- Test result summary:
  - `./gradlew test --tests "com.gitranker.api.global.logging.LogContextTest"` passed
  - `./gradlew test --tests "com.gitranker.api.infrastructure.github.GitHubApiErrorHandlerTest"` passed
- Follow-up tasks:
  - migrate remaining critical debug logs to `LogContext`-based structured logs
