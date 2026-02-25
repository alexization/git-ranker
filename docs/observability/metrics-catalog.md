# Metrics Catalog (M2-2)

## 1) Purpose
- Provide one source of truth for Git-Ranker reliability and product signals.
- Define explicit SLI targets so humans and agents evaluate incidents with the same criteria.

## 2) Core SLI Targets (Initial)
1. Availability (1h): `>= 99.0%`
- Query:
`(1 - (sum(rate(http_server_requests_seconds_count{application="git-ranker-api", status=~"5.."}[1h])) / sum(rate(http_server_requests_seconds_count{application="git-ranker-api"}[1h])))) * 100 or vector(100)`

2. Server Error Rate (5m): `< 1.0%`
- Query:
`sum(rate(http_server_requests_seconds_count{application="git-ranker-api", status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count{application="git-ranker-api"}[5m])) * 100 or vector(0)`

3. API Latency P99 (5m): `< 1000ms`
- Query:
`histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket{application="git-ranker-api"}[5m])) by (le)) * 1000`

4. GitHub API Success Rate (5m): `>= 98.0%`
- Query:
`sum(rate(github_api_calls_total{application="git-ranker-api", result="success"}[5m])) / sum(rate(github_api_calls_total{application="git-ranker-api"}[5m])) * 100`

5. Batch Success Rate (1d): `>= 95.0%`
- Query:
`sum(increase(batch_jobs_completed_total{application="git-ranker-api"}[1d])) / (sum(increase(batch_jobs_completed_total{application="git-ranker-api"}[1d])) + sum(increase(batch_jobs_failed_total{application="git-ranker-api"}[1d]))) * 100`

## 3) Domain Metrics
| Metric | Tags | Meaning | Source |
| --- | --- | --- | --- |
| `user_registrations_total` | `application` | Total user registrations | `BusinessMetrics` |
| `user_logins_total` | `application` | Total user logins | `BusinessMetrics` |
| `profile_views_total` | `application` | Total profile views | `BusinessMetrics` |
| `badge_views_total` | `application` | Total badge views | `BusinessMetrics` |
| `user_refreshes_total` | `application` | Total manual refreshes | `BusinessMetrics` |
| `user_deletions_total` | `application` | Total account deletions | `BusinessMetrics` |
| `errors_total` | `application`, `error_code` | Total errors by error code | `BusinessMetrics` |

## 4) Batch Metrics
| Metric | Tags | Meaning | Source |
| --- | --- | --- | --- |
| `batch_jobs_completed_total` | `application` | Total successful batch jobs | `BatchMetrics` |
| `batch_jobs_failed_total` | `application` | Total failed batch jobs | `BatchMetrics` |
| `batch_items_processed_total` | `application` | Total processed items | `BatchMetrics` |
| `batch_items_skipped_total` | `application` | Total skipped items | `BatchMetrics` |
| `batch_job_duration_seconds` | `application`, `status` | Batch execution duration (`success/failure`) | `BatchMetrics` |

## 5) External Integration (GitHub) Metrics
| Metric | Tags | Meaning | Source |
| --- | --- | --- | --- |
| `github_api_remaining` | `application` | Remaining GitHub API calls | `GitHubApiMetrics` |
| `github_api_reset_at_epoch` | `application` | Rate limit reset time (epoch) | `GitHubApiMetrics` |
| `github_api_cost_total` | `application` | Total GraphQL query cost | `GitHubApiMetrics` |
| `github_api_calls_total` | `application`, `result` | Calls by result (`success/failure/rate_limited`) | `GitHubApiMetrics` |
| `github_api_latency_seconds` | `application` | GitHub API latency | `GitHubApiMetrics` |

## 6) Platform Metrics (Actuator Defaults)
| Metric | Tags | Meaning |
| --- | --- | --- |
| `http_server_requests_seconds_*` | `application`, `uri`, `status`, `method` | API throughput, latency, and errors |
| `jvm_memory_*` | `application`, `area`, `id` | JVM memory usage |
| `process_cpu_usage` | `application` | Process CPU usage |
| `jvm_threads_live_threads` | `application` | Live thread count |

## 7) Dashboard Mapping
1. Service and domain dashboards:
- `dashboards/git-ranker-dashboard.json`

2. System dashboards:
- `dashboards/git-ranker-system-metrics.json`

## 8) Operational Checklist
1. When adding a new business event:
- add Counter/Timer
- update this catalog
- add or update dashboard query/panel

2. During PR validation:
- verify metric exposure from `/actuator/prometheus`
- attach metric query results (screenshot or values)
