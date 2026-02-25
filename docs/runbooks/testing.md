# Testing Runbook

## Goal
Run the right test scope quickly and record evidence in PR.

## Baseline Commands
1. Build only: `./gradlew build -x test`
2. Unit tests: `./gradlew test`
3. Integration tests: `./gradlew integrationTest`
4. Coverage verification: `./gradlew test jacocoTestCoverageVerification`
   - Current minimum line coverage gate: `45%` (bundle-level)

## Targeted Test Commands
1. Single unit class:
`./gradlew test --tests "com.gitranker.api.domain.user.service.UserRegistrationServiceTest"`
2. Single unit method:
`./gradlew test --tests "com.gitranker.api.domain.user.service.UserRegistrationServiceTest.should_fetchGitHubDataAndSave_when_newUser"`
3. Single integration class:
`./gradlew integrationTest --tests "com.gitranker.api.domain.user.UserRepositoryIT"`
4. Single integration method:
`./gradlew integrationTest --tests "com.gitranker.api.domain.user.UserRepositoryIT.should_findUser_when_nodeIdExists"`

## Selection Rule
1. Start with the smallest impacted test scope.
2. Run broader suite before merge.
3. For data/container-sensitive changes, include `integrationTest`.

## Evidence Format (PR)
- Command:
- Result: pass/fail
- Notes: skipped reason if not run
- For coverage checks, record the threshold result and report artifact path.

## Common Failures
- Docker unavailable: `integrationTest` may fail while `test` passes.
- Wrong method filter: use Java method names, not `@DisplayName` text.
