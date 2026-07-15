# STRUCTURE.md

`git-ranker` backend의 프로젝트 구조 가이드다. base package는 `com.gitranker.api`, 진입점은 `GitRankerApiApplication.java`다. Spring Boot 3.4 / Java 21 / Gradle.

## 패키지 구조

`domain/`은 package-by-feature로, 기능별 패키지 안에 controller·service·entity·repository를 콜로케이션한다. 별도의 최상위 `controller/`, `service/` 패키지는 두지 않는다.

```
com.gitranker.api
├── batch/            일일 점수 재계산 Spring Batch 파이프라인
├── domain/           비즈니스 기능 (package-by-feature)
├── global/           횡단 관심사 (설정, 인증, 에러, 로깅, 응답)
└── infrastructure/   외부 시스템 연동 (GitHub GraphQL)
```

### batch/ — 일일 점수 재계산

- `job/DailyScoreRecalculationJobConfig`: chunk 기반 Step(reader→processor→writer) + 랭킹 tasklet. `GitHubApiRetryableException`은 지수 백오프 재시도, `GitHubApiNonRetryableException`은 skip.
- `reader/UserItemReader`, `processor/ScoreRecalculationProcessor`, `writer/UserItemWriter`, `tasklet/RankingRecalculationTasklet`
- `listener/`: 진행률·GitHub 비용·skip 리스너. `metrics/BatchMetrics`.
- `scheduler/BatchScheduler`: 매일 자정(KST) 트리거. `spring.batch.job.enabled: false`이므로 job은 scheduler/수동으로만 실행된다.
- `strategy/`: 활동 갱신 전략 패턴 — `FullActivityUpdateStrategy`(전체 이력, 수동 refresh), `IncrementalActivityUpdateStrategy`(당해 연도, 일일 배치).

### domain/ — 기능별 패키지

| 패키지 | 책임 | 주요 클래스 |
|---|---|---|
| `auth/` | JWT + refresh token 세션 | `AuthController`, `RefreshToken(Repository)`, `service/AuthService`, `RefreshTokenCleanupScheduler` |
| `badge/` | 동적 SVG 티어 배지 | `BadgeController`, `BadgeService`, `SvgBadgeRenderer`, `TierGradientProvider` |
| `failure/` | 배치 실패 기록 | `BatchFailureLog(Repository/Service)` |
| `log/` | 일일 활동 스냅샷(전일 대비 diff) | `ActivityLog(Repository/Service)`, `ActivityLogOrchestrator` |
| `ranking/` | 랭킹 조회·재계산 | `RankingController`, `RankingService`, `RankingRecalculationService` |
| `user/` | 사용자·점수·티어 | `User`, `Tier`/`Role`(enum), `vo/`(`Score`, `RankInfo`, `ActivityStatistics`), 세분화된 `service/`(`UserQueryService`, `UserRegistrationService`, `UserRefreshService`, `UserDeletionService`, `UserPersistenceService`, `BaselineStatsCalculator`) |

### global/ — 횡단 관심사

- `auth/`: GitHub OAuth2(`CustomOAuth2UserService`, `OAuth2AuthenticationSuccessHandler`) + `jwt/`(`JwtProvider`, `JwtAuthenticationFilter`), `AuthCookieManager`
- `config/`: `SecurityConfig`, `CacheConfig`(Caffeine), `OAuth2ClientConfig`, `OpenApiConfig`, `MessageConfig`, `TimeZoneConfig`
- `error/`: `GlobalExceptionHandler`, `ErrorType`, `exception/`(`BusinessException`, GitHub 계열 예외)
- `logging/`: 구조화 로깅(`LoggingFilter`, `LogContext`, `LogSanitizer`), `aop/GitHubApiLoggingAspect`
- `response/`: `ApiResponse` 공통 envelope, `ResultType`
- `i18n/MessageUtils` + `messages_en/ko.properties`, `metrics/BusinessMetrics`, `util/`

### infrastructure/github/ — GitHub 연동

- `GitHubGraphQLClient`(GraphQL 호출), `GitHubActivityService`, `GitHubDataMapper`, `GitHubApiErrorHandler`, `GitHubApiMetrics`
- `token/GitHubTokenPool`: rate limit 대응 멀티 토큰 로테이션
- `dto/`, `util/GraphQLQueryBuilder`

## 네이밍 규약

- 클래스: `*Controller`, `*Service`, `*Repository`. 엔티티는 순수 명사(`User`, `RefreshToken`).
- DTO는 기능 패키지의 `dto/`, 임베디드 값 객체는 `vo/` 서브패키지.
- API prefix: `/api/v1/**`. 공통 응답은 `global/response/ApiResponse`.

## 테스트 구조

- `src/test/java/`는 main 패키지 트리를 그대로 미러링하고, 이름은 `<ClassName>Test`다. JUnit 5.
- 공용 픽스처는 `support/TestFixtures` 정적 팩토리(`user()`, `refreshToken()` 등)를 사용한다. 고정 상수 기반이라 결정적이다.
- integration test 소스셋은 없다. 검증 게이트는 `./gradlew test`와 `./gradlew build`뿐이다 (`CLAUDE.md` 참고).
- 테스트 레벨 선택 사다리(JUnit+Mockito → `@WebMvcTest` → `@DataJpaTest` → `@SpringBootTest`)는 `.claude/skills/red` 스킬이 정의한다.

## resources 구조

- `application.yml`: 공통 — JPA `ddl-auto: validate`, `spring.batch.job.enabled: false`, `batch.chunk-size: 100`, actuator는 management port **9090**, Asia/Seoul.
- `application-local.yml` / `application-prod.yml`: 프로파일별 datasource·OAuth·CORS·쿠키 정책 (`docs/DEVELOPMENT.md` 참고).
- `logback-spring.xml`: logstash JSON 인코더. `messages_en/ko.properties`: i18n.
