# Plan: 2026-02-25-m5-1-quality-gates-and-runtime-safety

## 1) Purpose
- Problem: workflow/process guardrails exist, but code-level quality confidence is still weak.
- Intended outcome: add automated coverage verification, stricter deployment health checks, and PII-safe logging defaults.
- Non-goals: redesign business domain flows or replace deployment architecture.

## 2) Scope
- In scope:
  - add JaCoCo verification tasks and PR quality workflow
  - harden deployment health check condition
  - reduce production actuator exposure defaults
  - enforce username masking defaults in logging context and high-risk raw logs
- Out of scope:
  - full migration of every plain logger call to structured logging
  - introducing external quality SaaS (Sonar, Codecov)

## 3) Progress
- [x] add M5 roadmap/playbook entries
- [x] add coverage gate in Gradle and CI workflow
- [x] harden deploy health check and production actuator scope
- [x] apply username masking defaults and add tests
- [x] run validation commands and record outcomes

## 4) Design Notes
- Constraints: keep the existing `build/test/integrationTest` split intact.
- Tradeoffs: start with a realistic baseline coverage threshold to avoid blocking all PRs.
- Open questions: final target coverage threshold after two to three weekly scorecards.

## 5) Decision Log
- 2026-02-25: M5 focuses on automated quality confidence, not process expansion.
- 2026-02-25: set initial line coverage gate at 45% to enforce a meaningful floor while staying below current baseline.

## 6) Validation Plan
- Required commands:
  - `./gradlew test`
  - `./gradlew test jacocoTestCoverageVerification`
- Observability checks:
  - verify logging contract and runtime behavior align for username masking

## 7) Risks and Rollback
- Risks: baseline threshold may be too strict or too lenient for current suite.
- Rollback strategy:
  - lower only the numeric coverage threshold in `build.gradle`
  - disable strict deploy health check condition in `deploy.yml` if false positives occur

## 8) Result Snapshot
- `./gradlew build -x test` passed
- `./gradlew test` passed
- `./gradlew test jacocoTestReport jacocoTestCoverageVerification` passed
- `./gradlew integrationTest` is environment-dependent (Docker/Testcontainers required)
