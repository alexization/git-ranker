---
name: red
description: git-ranker에서 테스트 코드 작성이나 TDD 기반 기능 구현을 시작할 때 첫 red turn을 수행한다. Java/Spring Boot behavior slice를 failing test로 고정하고, 최소 test level과 실패 원인을 잠가야 할 때 이 skill을 사용한다.
---

# Red Turn

이 turn의 목적은 기대 동작을 failing test 하나로 잠그는 것이다. production pass를 만들지 말고, 구현 공백이나 contract mismatch 때문에 실패하는 상태로 끝낸다.

## 먼저 확인할 것

- 사용자와 합의한 작업 범위 (Plan 모드 계획 또는 요청 내용)
- `CLAUDE.md`
- `build.gradle`
- target production package under `src/main/java`
- existing `src/test/` tree와 test dependency 존재 여부

## Test Level 선택 순서

1. pure JUnit + Mockito: 서비스, 계산기, orchestrator처럼 collaborator 경계가 분명할 때
2. `@WebMvcTest`: controller, request validation, response contract를 잠글 때
3. `@DataJpaTest`: repository query, entity mapping, persistence behavior를 잠글 때
4. `@SpringBootTest`: 더 좁은 slice로 behavior를 표현할 수 없을 때만

## 작업 방식

1. behavior slice를 하나로 줄인다. 메서드 하나, 엔드포인트 한 규칙, 배치 step 한 조건처럼 지금 turn에서 green으로 닫을 수 있는 범위만 잡는다.
2. production package를 미러링하는 test file 하나를 만든다 또는 수정한다.
3. assertion은 기대 contract를 직접 드러내게 쓴다. TODO, placeholder, 너무 넓은 smoke assertion으로 숨기지 않는다.
4. 새 기능을 TDD로 시작하는 경우, 의도적으로 없는 public API를 참조한 compile failure도 red evidence가 될 수 있다. 단, failure가 현재 slice를 명확히 설명해야 한다.
5. 현재 repo에 최소 test runtime이 없으면, 합의된 범위 안에서만 가장 작은 test-side bootstrap을 추가한다. 예: `spring-boot-starter-test`, `src/test/resources/application-test.yml`. 이 bootstrap은 failing test를 실행 가능하게 만드는 목적만 가져야 한다.
6. 가장 좁은 명령으로 red를 확인한다. 예: `./gradlew test --tests com.gitranker.api.domain.ranking.RankingServiceTest`
7. 실패 원인이 missing behavior인지, infra drift인지, 요구사항 어긋남인지 구분해 기록한다. infra drift가 주요 원인이면 green으로 밀어붙이지 말고 사용자에게 알리고 범위를 다시 합의한다.

## 결과

- failing test path
- red command
- expected failure reason
- next handoff: `green`

## 피해야 할 것

- production code를 수정하는 것
- 한 turn에 test file 여러 개를 만드는 것
- 더 좁은 slice가 가능한데 곧바로 `@SpringBootTest`로 가는 것
- green을 쉽게 만들기 위해 assertion을 약하게 쓰는 것
- 합의되지 않은 test infra rebuild를 red에 숨겨 섞는 것
