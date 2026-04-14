# AGENTS.md

`git-ranker`는 Git Ranker의 backend API 서버 저장소다. workflow repo는 orchestration만 소유하고, backend 구현과 verification/deploy entrypoint의 canonical source는 이 저장소가 직접 소유한다.

## 시작 순서

1. `README.md`에서 프로젝트 목적과 high-level overview를 확인한다.
2. `build.gradle`, `settings.gradle`에서 모듈 구조, 의존성, verification task를 확인한다.
3. `.github/workflows/ci.yml`, `.github/workflows/deploy.yml`에서 CI verification lane과 pre-deploy gate를 확인한다.
4. `src/test/java/`에서 current verification slice를 본다.
   - `*IT.java`는 Testcontainers 기반 integration lane이다.
5. `src/main/java/`, `src/main/resources/`에서 실제 구현과 runtime config를 읽는다.

## Source Of Truth

- repo overview: `README.md`
- build and verification entrypoint: `build.gradle`
- CI / deploy gate: `.github/workflows/ci.yml`, `.github/workflows/deploy.yml`
- backend behavior and contracts: `src/main/java/`, `src/test/java/`
- runtime container surface: `Dockerfile`, `docker-compose.yml`

## 운영 원칙

- backend 구현 세부사항은 workflow repo 문서가 아니라 이 저장소의 문서, 설정, 코드, 테스트가 canonical source다.
- root `README.md`는 개요만 유지한다. concrete bootstrap, verification, entrypoint 설명은 `AGENTS.md`와 named entry docs가 맡는다.
- 현재 verification 기본 surface는 `./gradlew test`, `./gradlew integrationTest`, `./gradlew build`다.
- `integrationTest`는 Docker/Testcontainers 전제를 가진다. 로컬 환경에서 Docker가 없으면 CI evidence와 함께 해석한다.
- verification lane이나 deploy gate를 바꾸면 `build.gradle`, workflow YAML, `AGENTS.md`를 함께 맞춘다.
- 현재 repo-local `.codex/skills/`는 없다. 별도 skill이 없을 때는 이 문서와 nearest code/test를 먼저 읽고 진행한다.
