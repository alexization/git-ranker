## 1) 요약
- 무엇이 변경되었나요?
- 왜 지금 필요한가요?

## 2) 연관 이슈
- Closes #

## 3) 의도 명세
- 문제:
- 목표 동작:
- 비목표:

## 4) System of Record 링크
- Plan (머지 전 최종 상태): `docs/plans/completed/YYYY-MM-DD-<slug>.md`
- ADR (필요 시): `docs/adr/...`
- 관련 runbook/docs 업데이트:

## 5) 변경 범위
- 변경된 파일/패키지:
- API/스키마/동작 영향:

## 6) 검증 증거

| 유형 | 명령어 | 결과 |
| --- | --- | --- |
| Build | `./gradlew build -x test` | |
| Unit | `./gradlew test` | |
| Integration | `./gradlew integrationTest` 또는 `미실행(사유)` | |
| Quality (Coverage) | `./gradlew test jacocoTestCoverageVerification` 또는 `미실행(사유)` | |

## 7) AI 리뷰 루프 증거

| 리뷰어 | 라운드 | 최종 결과 | 증거 링크 |
| --- | --- | --- | --- |
| Codex |  |  |  |
| CodeRabbitAI |  |  |  |

## 8) 관측성 확인
- 확인한 로그:
- 확인한 메트릭:
- 확인한 대시보드/쿼리:

## 9) 리스크 및 롤백
- 리스크:
- 롤백 계획:

## 10) Merge Ready Gate
- [ ] Codex/CodeRabbit final re-reviews completed
- [ ] All review findings triaged (fixed or justified)
- [ ] Plan moved to `docs/plans/completed/...` in this PR
- [ ] Risk and rollback section updated

## 11) 체크리스트
- [ ] 이슈가 연결되어 있음
- [ ] Plan 문서가 연결되어 있음
- [ ] 검증 증거가 포함되어 있음
- [ ] 문서가 업데이트되었거나 불필요 사유가 명시되어 있음
