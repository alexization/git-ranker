# DEVELOPMENT.md

로컬 개발 환경 가이드다. 프로젝트 구조는 [STRUCTURE.md](STRUCTURE.md), 배포/모니터링은 [OPERATIONS.md](OPERATIONS.md)를 본다.

## 요구 사항

- Java 21 (toolchain이 강제한다)
- MySQL 8.0 (localhost:3306, 데이터베이스 `git_ranker`)
- GitHub OAuth App + GitHub API 토큰

## 프로파일

활성 프로파일은 `SPRING_PROFILES_ACTIVE`로 정한다.

| 프로파일 | 용도 | 특징 |
|---|---|---|
| `local` | 로컬 개발 | MySQL, OAuth·DB env(`.env.local`), CORS localhost:3000, 쿠키 non-secure |
| `prod` | 운영 | `DB_URL` 등 운영 env, git-ranker.com CORS, secure 쿠키, actuator 노출 축소 |

## 필요 환경 변수 (local)

`local`·`prod`가 같은 변수명을 쓰고, 값은 환경별 파일로 구분한다. 로컬 도커는 `--env-file .env.local`로 주입한다(템플릿 `.env.local.example`).

```text
DB_NAME, DB_USERNAME, DB_PASSWORD
GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET, GITHUB_REDIRECT_URI
JWT_SECRET, JWT_ACCESS_TOKEN_EXPIRATION, JWT_REFRESH_TOKEN_EXPIRATION
GITHUB_API_TOKENS   # GitHub GraphQL 토큰 (콤마 구분, 토큰 풀 로테이션)
```

## 실행

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun   # 호스트 직접 실행: 아래 도커 권장. 이 경로는 spring-dotenv가 읽는 .env(로컬 변수 전체)가 필요
./gradlew test                                    # 단위 테스트
./gradlew build                                   # 패키징 (CI는 build -x test)
```

로컬 도커(api + db만, `local` 프로파일):

```bash
docker compose --env-file .env.local -f docker-compose.local.yml up -d --build   # 기동
docker compose --env-file .env.local -f docker-compose.local.yml down            # 정지(-v 추가 시 DB 볼륨 삭제)
```

운영 전체 스택(api + db + 모니터링)은 `docker compose up -d`. 서비스: `git-ranker-api`(8080, mgmt 9090), `git-ranker-db`(MySQL 8.0), `prometheus`, `loki`, `promtail`, `grafana`(3001).

## 주요 엔드포인트

- API: `http://localhost:8080/api/v1/**`
- Swagger UI: springdoc 기반, `/api/v1/**` 스캔 (`global/config/OpenApiConfig`)
- Actuator (management port 9090): `/actuator/health`, `/actuator/prometheus` 등

## 검증 베이스라인

`./gradlew test` → `./gradlew build`. 구현 변경에는 테스트 변경을 동반한다(`.githooks/pre-commit`이 검사, 예외는 `TDD_SKIP=1` + 커밋 메시지 근거). TDD 작업 절차는 `.claude/skills/`의 `red`/`green`/`refactor`를 따른다.
