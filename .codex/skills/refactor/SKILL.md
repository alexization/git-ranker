---
name: refactor
description: `git-ranker`에서 green 상태의 TDD slice를 유지한 채 구조만 정리하는 refactor turn을 수행한다. Java/Spring Boot production/test code의 중복 제거, 이름 개선, helper 추출 같은 정리를 하고 같은 verification을 green으로 끝내야 할 때 이 skill을 사용한다.
---

# Refactor Turn

refactor는 의미를 바꾸지 않는 정리다. contract나 observable behavior가 달라질 것 같으면 refactor를 멈추고 slice를 다시 나눈다.

## 먼저 확인할 것

- green evidence
- current slice의 남은 smell
- approved spec
- 방금 green으로 만든 test command

## 작업 방식

1. 줄일 냄새를 먼저 하나로 적는다. 예: 중복된 mapping, 읽기 어려운 메서드 이름, 테스트 setup 잡음, fixture 반복.
2. observable behavior를 바꾸지 않는 범위에서 production code와 test code를 정리한다.
3. test code를 건드린다면 의미 변경 없는 cleanup으로 한정한다. helper 추출, builder 정리, 중복 제거는 가능하지만 assertion intent나 시나리오 범위를 바꾸지 않는다.
4. 같은 targeted command를 다시 실행해 green을 유지한다.
5. shared area를 건드렸다면 가장 가까운 작은 suite만 추가 확인한다. 현재 repo baseline이 build-only라고 해서 refactor turn을 전체 verification rebuild로 넓히지 않는다.
6. 이번 slice가 끝났는지, 다음 red slice가 남았는지 짧게 정리한다.

## 결과

- 제거한 smell
- rerun command와 green evidence
- 정리한 path
- next slice 또는 stop signal

## 피해야 할 것

- 새 behavior를 추가하는 것
- bug fix를 refactor로 위장하는 것
- assertion 의미나 public contract를 바꾸는 것
- 현재 slice와 무관한 넓은 architecture cleanup으로 번지는 것
- green이 깨진 상태로 turn을 끝내는 것
