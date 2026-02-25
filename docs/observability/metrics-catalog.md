# 메트릭 카탈로그 (M2-2)

## 1) 목적
- Git-Ranker의 핵심 품질 신호를 하나의 문서에서 조회할 수 있도록 정리한다.
- 에이전트와 사람이 동일한 기준으로 이상 징후를 판단하도록 SLI 목표를 명시한다.

## 2) 핵심 SLI 목표 (초기값)
1. 가용성(1h): `>= 99.0%`
- Query:
`(1 - (sum(rate(http_server_requests_seconds_count{application="git-ranker-api", status=~"5.."}[1h])) / sum(rate(http_server_requests_seconds_count{application="git-ranker-api"}[1h])))) * 100 or vector(100)`

2. 서버 오류율(5m): `< 1.0%`
- Query:
`sum(rate(http_server_requests_seconds_count{application="git-ranker-api", status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count{application="git-ranker-api"}[5m])) * 100 or vector(0)`

3. API 지연시간 P99(5m): `< 1000ms`
- Query:
`histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket{application="git-ranker-api"}[5m])) by (le)) * 1000`

4. GitHub API 성공률(5m): `>= 98.0%`
- Query:
`sum(rate(github_api_calls_total{application="git-ranker-api", result="success"}[5m])) / sum(rate(github_api_calls_total{application="git-ranker-api"}[5m])) * 100`

5. 배치 성공률(1d): `>= 95.0%`
- Query:
`sum(increase(batch_jobs_completed_total{application="git-ranker-api"}[1d])) / (sum(increase(batch_jobs_completed_total{application="git-ranker-api"}[1d])) + sum(increase(batch_jobs_failed_total{application="git-ranker-api"}[1d]))) * 100`

## 3) 도메인 메트릭
| 메트릭 | 태그 | 의미 | 소스 |
| --- | --- | --- | --- |
| `user_registrations_total` | `application` | 회원 등록 누적 수 | `BusinessMetrics` |
| `user_logins_total` | `application` | 로그인 누적 수 | `BusinessMetrics` |
| `profile_views_total` | `application` | 프로필 조회 누적 수 | `BusinessMetrics` |
| `badge_views_total` | `application` | 배지 조회 누적 수 | `BusinessMetrics` |
| `user_refreshes_total` | `application` | 수동 갱신 누적 수 | `BusinessMetrics` |
| `user_deletions_total` | `application` | 회원 탈퇴 누적 수 | `BusinessMetrics` |
| `errors_total` | `application`, `error_code` | 에러 코드별 누적 수 | `BusinessMetrics` |

## 4) 배치 메트릭
| 메트릭 | 태그 | 의미 | 소스 |
| --- | --- | --- | --- |
| `batch_jobs_completed_total` | `application` | 배치 성공 횟수 | `BatchMetrics` |
| `batch_jobs_failed_total` | `application` | 배치 실패 횟수 | `BatchMetrics` |
| `batch_items_processed_total` | `application` | 처리 아이템 누적 수 | `BatchMetrics` |
| `batch_items_skipped_total` | `application` | 스킵 아이템 누적 수 | `BatchMetrics` |
| `batch_job_duration_seconds` | `application`, `status` | 배치 실행 시간(`success/failure`) | `BatchMetrics` |

## 5) 외부 연동(GitHub) 메트릭
| 메트릭 | 태그 | 의미 | 소스 |
| --- | --- | --- | --- |
| `github_api_remaining` | `application` | 남은 API 호출량 | `GitHubApiMetrics` |
| `github_api_reset_at_epoch` | `application` | Rate Limit 리셋 시각(epoch) | `GitHubApiMetrics` |
| `github_api_cost_total` | `application` | GraphQL 쿼리 cost 누적치 | `GitHubApiMetrics` |
| `github_api_calls_total` | `application`, `result` | 호출 결과별 누적 수(`success/failure/rate_limited`) | `GitHubApiMetrics` |
| `github_api_latency_seconds` | `application` | API 호출 지연시간 | `GitHubApiMetrics` |

## 6) 플랫폼 메트릭(Actuator 기본)
| 메트릭 | 태그 | 의미 |
| --- | --- | --- |
| `http_server_requests_seconds_*` | `application`, `uri`, `status`, `method` | API 처리량/지연/오류 |
| `jvm_memory_*` | `application`, `area`, `id` | JVM 메모리 사용량 |
| `process_cpu_usage` | `application` | 프로세스 CPU 사용률 |
| `jvm_threads_live_threads` | `application` | 라이브 스레드 수 |

## 7) 대시보드 매핑
1. 서비스/도메인 지표:
- `dashboards/git-ranker-dashboard.json`

2. 시스템 지표:
- `dashboards/git-ranker-system-metrics.json`

## 8) 운영 체크리스트
1. 새로운 비즈니스 이벤트를 추가하면:
- Counter/Timer 추가
- 본 문서 카탈로그 업데이트
- 대시보드 패널 또는 쿼리 추가

2. PR 검증 시:
- `/actuator/prometheus`에서 메트릭 노출 확인
- 변경 메트릭에 대한 쿼리 결과 스크린샷 또는 수치 첨부
