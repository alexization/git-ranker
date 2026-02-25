# Weekly Scorecard: 2026-W09

- Review window: 2026-02-23 to 2026-03-01
- Owner: @hyoseok
- Related issue: TBD

## 1) Delivery Throughput
| Metric | Target | Actual | Notes |
| --- | --- | --- | --- |
| PRs merged |  | N/A | Branch work in progress; merge count not recorded in this file yet |
| Median PR lead time |  | N/A | To be measured from merged PR timestamps |
| Reopened PR count | 0 | N/A | No merged/reopened PR data collected in this review |

## 2) Reliability
| Metric | Target | Actual | Notes |
| --- | --- | --- | --- |
| Availability (1h) | >= 99.0% | N/A | Not sampled this week |
| Server error rate (5m) | < 1.0% | N/A | Not sampled this week |
| API latency P99 | < 1000ms | N/A | Not sampled this week |
| Batch success rate (1d) | >= 95.0% | N/A | Not sampled this week |

## 3) Architecture Guardrails
| Check | Status | Notes |
| --- | --- | --- |
| ArchUnit tests passing | Passed | `ArchitectureGuardrailTest` added and passing |
| Layer boundary violations | None detected | Current rules passed on baseline code |
| Controller placement violations | None detected | `@RestController` / `@RestControllerAdvice` placement checks passed |

## 4) Observability and Logging
| Check | Status | Notes |
| --- | --- | --- |
| Metrics catalog up to date | Yes | M2-2 metrics catalog was added |
| Logging contract up to date | Yes | M2-3 logging contract and tests were added |
| Dashboard query drift detected | Unknown | No dedicated dashboard drift review run this week |

## 5) Documentation Drift
| Check | Status | Notes |
| --- | --- | --- |
| Active plans updated during execution | Yes | Plans were maintained and then archived |
| Completed plans moved after merge | Yes | Completed plans moved to `docs/plans/completed` |
| ADR needed but missing | Unknown | No new architecture decision flagged after M4-2 |
| Docs/code mismatch found | None detected | No mismatch discovered during this cycle |

## 6) Incident and Regression Review
- New incidents this week: None recorded.
- Regressions caught by tests/CI: None recorded.
- Escaped defects: None recorded.

## 7) Top Risks for Next Week
1. Weekly scorecard could become checklist-only unless reliability values are actually sampled.
2. ArchUnit guardrails are still baseline-level and may miss deeper dependency drift.
3. Manual process steps may be skipped without ownership reminders.

## 8) Action Items
| Item | Owner | Due | Related issue/plan |
| --- | --- | --- | --- |
| Define data source for PR lead-time and reopened PR metrics | @hyoseok | 2026-03-06 | TBD |
| Add one stricter ArchUnit rule candidate and run impact check | @hyoseok | 2026-03-06 | docs/plans/completed/2026-02-25-m4-2-architecture-guardrail.md |
| Run first dashboard drift review using metrics/logging contracts | @hyoseok | 2026-03-06 | docs/observability/metrics-catalog.md |

## 9) Reference Links
- Merged PRs: TBD
- Related plans:
  - `docs/plans/completed/2026-02-25-harness-bootstrap.md`
  - `docs/plans/completed/2026-02-25-m2-2-metrics-catalog.md`
  - `docs/plans/completed/2026-02-25-m2-3-logging-contract.md`
  - `docs/plans/completed/2026-02-25-m4-2-architecture-guardrail.md`
  - `docs/plans/completed/2026-02-25-m4-3-weekly-scorecard.md`
- Related ADRs:
  - `docs/adr/ADR-0001-harness-operating-model.md`
- Dashboards and queries:
  - `dashboards/git-ranker-dashboard.json`
  - `dashboards/git-ranker-system-metrics.json`
