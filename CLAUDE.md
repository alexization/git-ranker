# CLAUDE.md

`git-ranker`는 Git Ranker의 backend API 서버 저장소다. umbrella 저장소(`git-ranker-workflow`)는 cross-repo 진입점만 소유하고, backend 구현과 verification/deploy entrypoint의 canonical source는 이 저장소가 직접 소유한다.

## 시작 순서

1. `README.md`에서 프로젝트 목적과 high-level overview를 확인한다.
2. `docs/STRUCTURE.md`에서 패키지 구조와 네이밍 규약을 확인한다.
3. `build.gradle`, `settings.gradle`에서 모듈 구조, 의존성, verification task를 확인한다.
4. 로컬 실행/환경 변수가 필요하면 `docs/DEVELOPMENT.md`, 배포/모니터링이면 `docs/OPERATIONS.md`를 본다.
5. 테스트 코드 작성이나 TDD 기반 기능 구현 요청이면 `.claude/skills/`의 `red`, `green`, `refactor` skill을 먼저 확인한다.
6. `src/main/java/`, `src/main/resources/`에서 실제 구현과 runtime config를 읽는다.

## Source Of Truth

- repo overview: `README.md`
- 패키지 구조·네이밍·테스트 구조: `docs/STRUCTURE.md`
- 로컬 개발·env·실행: `docs/DEVELOPMENT.md`
- 배포 파이프라인·모니터링: `docs/OPERATIONS.md`
- build and verification entrypoint: `build.gradle`
- CI / deploy gate: `.github/workflows/ci.yml`, `.github/workflows/deploy.yml`
- repo-local TDD workflow: `.claude/skills/red/SKILL.md`, `.claude/skills/green/SKILL.md`, `.claude/skills/refactor/SKILL.md`
- backend behavior and contracts: `src/main/java/`, `src/main/resources/`
- runtime container surface: `Dockerfile`, `docker-compose.yml`

## Skills

`.claude/skills/`는 두 계열로 구성된다.

- **프로세스 스킬 (repo 고유)**: `red` → `green` → `refactor` TDD 턴. 아래 TDD 워크플로 참고.
- **지식 스킬 (ECC에서 선별, MIT — [affaan-m/ECC](https://github.com/affaan-m/ECC))**: `springboot-patterns`(아키텍처·관측성), `springboot-security`(OAuth2/JWT), `jpa-patterns`(엔티티·쿼리·트랜잭션), `springboot-tdd`(JUnit5/Mockito/MockMvc 기법), `java-coding-standards`, `api-design`(REST 설계). 해당 영역 작업 시 자동 참조된다.

Spring Batch 전용 스킬은 없다. batch 규약은 `docs/STRUCTURE.md`의 batch 섹션이 canonical이다.

## 운영 원칙

- backend 구현 세부사항은 umbrella repo 문서가 아니라 이 저장소의 문서, 설정, 코드, 테스트가 canonical source다.
- root `README.md`는 개요만 유지한다. concrete bootstrap, verification, entrypoint 설명은 `CLAUDE.md`와 named entry docs가 맡는다.
- 현재 verification baseline command set은 `./gradlew test`, `./gradlew build`다.
- current baseline은 unit-test gate를 포함하지만 dedicated integration lane은 없다. follow-up rebuild가 필요하면 `build.gradle`, workflow YAML, `CLAUDE.md`를 함께 갱신한 뒤 repo-local baseline으로 다시 도입한다.
- CI와 deploy verification에서 `./gradlew test`를 먼저 실행한 뒤 packaging build는 `./gradlew build -x test`로 수행해 test suite를 중복 실행하지 않는다.
- verification lane이나 deploy gate를 바꾸면 `build.gradle`, workflow YAML, `CLAUDE.md`를 함께 맞춘다.

## TDD 워크플로

- repo-local TDD workflow는 `.claude/skills/`의 `red`, `green`, `refactor` skill이 소유한다.
- 테스트 코드 작성 요청이나 TDD 기반 기능 구현 요청이 들어오면 기본 순서를 `red -> green -> refactor`로 고정한다.
- `red`는 failing test와 현재 slice를 잠그고, `green`은 최소 production 변경으로 pass를 만들고, `refactor`는 green을 유지한 채 구조만 정리한다.
- 각 turn은 현재 slice 하나만 다루고, 다음 turn의 역할을 침범하지 않는다.
- unit test baseline을 바꾸거나 test dependency를 조정하는 작업은 비자명한 범위 변경이므로, Plan 모드나 AskUserQuestion으로 사용자와 먼저 합의한 뒤 `build.gradle`, workflow YAML, `CLAUDE.md`를 함께 갱신한다.
- skill은 test infra drift를 숨기지 말고 드러내야 한다.

## Guard

- `.githooks/pre-commit`: `src/main/` 변경을 테스트 변경 없이 커밋하면 실패한다. 의도적 예외는 `TDD_SKIP=1 git commit ...`으로 우회하되 근거를 커밋 메시지에 남긴다. 활성화: `git config core.hooksPath .githooks`
- `.claude/hooks/block-dangerous.sh`: Claude 세션 안에서 destructive 명령(rm -rf, force push, reset --hard 등)을 차단한다.
