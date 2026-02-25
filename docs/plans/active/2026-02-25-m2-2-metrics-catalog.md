# Plan: 2026-02-25-m2-2-metrics-catalog

## 1) Purpose
- Problem: 메트릭이 코드/대시보드에 분산돼 있어 운영 기준을 빠르게 파악하기 어렵다.
- Intended outcome: Git-Ranker 메트릭 카탈로그와 SLI 목표를 문서로 고정한다.
- Non-goals: 메트릭 수집 코드 리팩터링, 알림 정책 변경.

## 2) Scope
- In scope:
  - `docs/observability/metrics-catalog.md`
  - `docs/index.md`, `AGENTS.md`, runbook 링크 정합성
- Out of scope:
  - 신규 메트릭 코드 추가
  - 대시보드 구조 개편

## 3) Progress
- [x] 코드/대시보드 기준 메트릭 목록 수집
- [x] SLI 초기 목표 정의
- [x] 카탈로그 문서 작성 및 인덱스 연결

## 4) Design Notes
- Constraints: 현재 운영 대시보드/코드에 존재하는 메트릭 기준으로만 기술한다.
- Tradeoffs: 초기 SLI 목표는 보수적으로 제시하고 추후 운영 데이터로 조정한다.
- Open questions: GitHub API 성공률 기준(98%)의 장기 적정성.

## 5) Decision Log
- 2026-02-25: SLI는 `가용성/오류율/지연시간/GitHub 성공률/배치성공률` 5개 축으로 시작.
- 2026-02-25: 메트릭 카탈로그의 source는 코드 클래스 단위로 명시.

## 6) Validation Plan
- Required commands:
  - docs-only change, runtime command not required
- Observability checks:
  - 메트릭 명은 코드(`BusinessMetrics`, `BatchMetrics`, `GitHubApiMetrics`)와 대시보드 쿼리로 대조

## 7) Risks and Rollback
- Risks: 일부 SLI 임계치가 실제 운영 트래픽 특성과 불일치할 수 있음.
- Rollback strategy: 임계치를 문서에서 단계적으로 조정(코드 영향 없음).

## 8) Result Summary
- What changed: M2-2 메트릭 카탈로그와 SLI 문서가 추가됨.
- Test result summary: docs-only change.
- Follow-up tasks:
  - M2-3에서 구조화 로그 필드 계약 문서/코드 정렬
