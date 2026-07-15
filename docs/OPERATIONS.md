# OPERATIONS.md

배포 파이프라인과 모니터링 스택 가이드다.

## CI (.github/workflows/ci.yml)

`main`/`develop` push·PR에서 실행: Java 21(temurin) → `./gradlew test` → `./gradlew build -x test`. 테스트를 먼저 돌리고 패키징은 `-x test`로 중복 실행을 피한다.

## 배포 (.github/workflows/deploy.yml)

`main` push, `v*` 태그, 수동 dispatch에서 실행. 3단계:

1. `verify`: CI와 동일한 test + build
2. `docker`: Buildx로 이미지 빌드 → Docker Hub push (latest/sha/semver 태그, gha 캐시)
3. `deploy`: SSH로 서버 접속 → `docker compose pull` + `up -d --force-recreate git-ranker-api` → 컨테이너 헬스체크 루프(최대 12회) → `127.0.0.1:9090/actuator/health` 확인 → `docker image prune`

롤백: 이전 sha 태그 이미지를 pull 해 동일 절차로 재기동한다.

## 모니터링 스택 (docker-compose)

| 서비스 | 역할 | 포트 |
|---|---|---|
| `prometheus` | 메트릭 수집 (30일 보존), `/actuator/prometheus` 스크레이프 | 내부 |
| `loki` + `promtail` | 컨테이너 로그 수집·저장 | 내부 |
| `grafana` | 대시보드 + unified alerting (Discord webhook) | 3001 |

설정 파일 위치 (repo 루트):

- `prometheus.yml`, `loki-config.yml`, `promtail-config.yml`
- `datasource.yml`, `dashboard.yml`: Grafana 프로비저닝
- `dashboards/`: `git-ranker-dashboard.json`(비즈니스), `git-ranker-system-metrics.json`(시스템)
- `alerting/`: `alert-rules.yml`, `alerting.yml`

## 관측 지점

- 애플리케이션 메트릭: Micrometer → `/actuator/prometheus` (http 히스토그램·퍼센타일 활성)
- 커스텀 메트릭: `global/metrics/BusinessMetrics`, `batch/metrics/BatchMetrics`, `infrastructure/github/GitHubApiMetrics`
- 구조화 로그: logback + logstash JSON 인코더 → promtail → loki
- 배치 실패: `domain/failure/BatchFailureLog` 테이블 + Grafana 알림
