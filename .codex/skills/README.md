# Skills

이 디렉터리는 `git-ranker` backend repo에서만 쓰는 repo-local skill을 관리한다.

## Canonical Ownership

- backend behavior, package structure, runtime config는 `AGENTS.md`, `README.md`, `src/main/`이 canonical source다.
- build/test baseline과 verification lane은 `build.gradle`, `.github/workflows/ci.yml`, `.github/workflows/deploy.yml`이 canonical source다.
- skill은 위 문서를 대체하지 않고, 반복되는 TDD turn의 역할과 handoff만 고정한다.

## Registry Status

- `red`: failing test로 behavior slice를 잠그는 첫 TDD turn
- `green`: red에서 잠근 failing test를 최소 production 변경으로 통과시키는 turn
- `refactor`: green을 유지한 채 구조와 중복만 정리하는 turn

## Recommended Use

1. `git-ranker`에서 테스트 코드를 추가하거나 backend 기능을 TDD로 구현할 때는 기본적으로 `red -> green -> refactor` 순서를 따른다.
2. 각 turn은 현재 slice 하나만 다루고, 다음 turn의 역할을 침범하지 않는다.
3. 현재 baseline에 필요한 test dependency, `src/test` bootstrap, verification lane 변경이 보이면 그 범위를 approved spec에 먼저 잠근다.
4. repo baseline이 아직 build-only인 상태이므로, skill은 test infra drift를 숨기지 말고 드러내야 한다.
