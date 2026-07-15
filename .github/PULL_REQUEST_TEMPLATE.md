## 1) 요약
- 무엇이 변경되었나요?
- 왜 지금 필요한가요?

## 2) 연관 이슈
- Closes #
- 관련 frontend 이슈/PR:

## 3) 문제와 목표
- 문제:
- 사용자/운영자 관점의 결과:
- 비목표:

## 4) 영향 범위
- 변경된 패키지/모듈:
- API/DTO/Schema 영향:
- DB/Cache/Batch/Scheduler 영향:
- 보안/권한 영향:

## 5) 검증 증거

| 유형 | 명령어 / 증거 | 결과 |
| --- | --- | --- |
| Build | `./gradlew build -x test` | |
| Unit | `./gradlew test` | |
| Integration | `./gradlew integrationTest` 또는 `미실행(<사유>)` | |
| Coverage | `./gradlew test jacocoTestCoverageVerification` 또는 `미실행(<사유>)` | |
| API/Manual Smoke | `curl ...` / Postman / 배치 실행 기록 또는 `미실행(<사유>)` | |

## 6) 관측성 확인
- 확인한 로그:
- 확인한 메트릭:
- 확인한 trace/dashboard/query:

## 7) AI 리뷰 메모 (선택)
- Claude Code:
- CodeRabbitAI:

## 8) 리스크 및 롤백
- 리스크:
- 롤백 계획:

## 9) 체크리스트
- [ ] 연관 이슈가 연결되어 있음
- [ ] Build / Unit / Integration 결과가 기입되어 있음
- [ ] API/스키마/배치 영향이 반영되었거나 없음을 명시함
- [ ] 로그/메트릭/trace 확인 내용을 적었거나 불필요 사유를 적음
- [ ] 문서 또는 후속 이슈가 업데이트되었거나 불필요 사유를 적음

