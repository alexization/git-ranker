# AGENTS.md

`git-ranker`는 Git Ranker의 backend API 서버 저장소다. workflow repo는 orchestration만 소유하고, backend 구현과 verification/deploy entrypoint의 canonical source는 이 저장소가 직접 소유한다.

## 시작 순서

1. `README.md`에서 프로젝트 목적과 high-level overview를 확인한다.
2. `build.gradle`, `settings.gradle`에서 모듈 구조, 의존성, verification task를 확인한다.
3. `.github/workflows/ci.yml`, `.github/workflows/deploy.yml`에서 CI verification lane과 pre-deploy gate를 확인한다.
4. 테스트 코드 작성이나 TDD 기반 기능 구현 요청이면 `.codex/skills/README.md`와 `red`, `green`, `refactor` skill을 먼저 확인한다.
5. 현재 `src/test/` tree는 baseline reset 상태이므로, follow-up verification rebuild 전까지는 `build.gradle`과 workflow가 retained build-only lane을 어떻게 고정하는지 먼저 본다.
6. `src/main/java/`, `src/main/resources/`에서 실제 구현과 runtime config를 읽는다.

## Source Of Truth

- repo overview: `README.md`
- build and verification entrypoint: `build.gradle`
- CI / deploy gate: `.github/workflows/ci.yml`, `.github/workflows/deploy.yml`
- repo-local TDD workflow: `.codex/skills/README.md`, `.codex/skills/red/SKILL.md`, `.codex/skills/green/SKILL.md`, `.codex/skills/refactor/SKILL.md`
- backend behavior and contracts: `src/main/java/`, `src/main/resources/`
- runtime container surface: `Dockerfile`, `docker-compose.yml`

## 운영 원칙

- backend 구현 세부사항은 workflow repo 문서가 아니라 이 저장소의 문서, 설정, 코드, 테스트가 canonical source다.
- root `README.md`는 개요만 유지한다. concrete bootstrap, verification, entrypoint 설명은 `AGENTS.md`와 named entry docs가 맡는다.
- 현재 verification baseline command set은 `./gradlew build`다.
- current baseline에는 dedicated test/integration lane이 없다. follow-up rebuild가 필요하면 `build.gradle`, workflow YAML, `AGENTS.md`를 함께 갱신한 뒤 repo-local baseline으로 다시 도입한다.
- verification lane이나 deploy gate를 바꾸면 `build.gradle`, workflow YAML, `AGENTS.md`를 함께 맞춘다.
- repo-local TDD workflow는 `.codex/skills/red`, `.codex/skills/green`, `.codex/skills/refactor`가 소유한다.
- 테스트 코드 작성 요청이나 TDD 기반 기능 구현 요청이 들어오면 기본 순서를 `red -> green -> refactor`로 고정한다.
- `red`는 failing test와 현재 slice를 잠그고, `green`은 최소 production 변경으로 pass를 만들고, `refactor`는 green을 유지한 채 구조만 정리한다.
- 현재 baseline이 build-only 상태이므로, test dependency 추가나 verification lane rebuild가 필요하면 그 범위를 spec에 먼저 잠그고 `build.gradle`, workflow YAML, `AGENTS.md`를 함께 갱신한다.
- repo-local skill로 해결되지 않는 작업만 이 문서와 nearest code/test를 추가로 읽고 진행한다.
