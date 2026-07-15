---
name: green
description: git-ranker에서 red turn으로 잠근 failing test를 최소 production-side 변경으로 통과시키는 green turn을 수행한다. Spring Boot production code를 좁은 범위로 수정하고 같은 command를 green으로 바꿔야 할 때 이 skill을 사용한다.
---

# Green Turn

이 turn의 목적은 pass를 만드는 것이다. 설계 정리나 다음 기능을 같이 하지 말고, red에서 잠근 slice만 green으로 닫는다.

## 먼저 확인할 것

- red turn의 failing evidence
- 사용자와 합의한 작업 범위
- target test file과 failure reason
- 관련 production package under `src/main/java`

## 작업 방식

1. red command를 다시 실행해 같은 failure가 재현되는지 확인한다.
2. 현재 slice를 통과시키는 최소 production-side 변경만 적용한다. 기본 대상은 `src/main/java`이며, 현재 behavior contract에 꼭 필요한 경우에만 `src/main/resources`를 건드린다.
3. test file은 그대로 둔다. passing을 위해 assertion, fixture, mock, request payload를 뒤로 숨겨 바꾸지 않는다.
4. Spring Boot에서는 가능한 한 가장 안쪽 계층부터 해결한다. domain/service 수정만으로 닫을 수 있으면 controller/config까지 넓히지 않는다.
5. 같은 targeted command를 다시 실행해 green을 만든다.
6. 영향 범위가 바로 옆 클래스나 slice test에 걸치면 작은 인접 suite까지만 추가 확인한다. green turn을 verification rebuild 작업으로 확장하지 않는다.
7. 남은 냄새, 중복, 이름 문제를 짧게 정리해 `refactor`로 넘긴다.

## 결과

- green command
- pass evidence
- 변경한 production path
- refactor candidate
- next handoff: `refactor`

## 피해야 할 것

- test file을 수정해 red를 없애는 것
- refactor 성격의 cleanup을 섞는 것
- 현재 failing slice와 무관한 새 behavior를 같이 구현하는 것
- 더 작은 수정으로 닫을 수 있는데 구조 개편이나 새 의존성 도입으로 넓히는 것
- green 과정에서 범위 어긋남이나 infra gap이 드러났는데도 사용자에게 알리지 않고 그대로 진행하는 것
