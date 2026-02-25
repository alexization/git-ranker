# Spec Gate Policy

This policy defines when implementation can start and how request specs are finalized.

## 1) Scope
- Applies to all feature, bugfix, refactor, and infra/workflow requests.
- Applies to both human and AI-agent driven tasks.

## 2) Execution Gate
- Implementation is blocked until spec status is `Approved`.
- Before approval, only read-only investigation is allowed.
- Code edits, commits, and PR implementation updates are allowed only after approval.
- Plan lifecycle mode is fixed to practical mode: update `active -> completed` in the same PR before final merge.

## 3) Required Fields (Definition of Ready)
- `Type`: feature | bugfix | refactor | infra
- `Goal`: problem and intended outcome
- `Scope`: in-scope and out-of-scope
- `Constraints`: architecture, security/PII, performance/SLO, compatibility, docs updates
- `Acceptance Criteria`: measurable pass conditions
- `Validation Plan`: commands and runtime checks (logs/metrics/health)
- `Risks and Rollback`: known risks and fallback strategy

## 4) Clarification Loop
- If required fields are incomplete, ask focused clarification questions.
- Clarification questions to the user must be written in Korean.
- Ask 1 to 3 questions per round, then update spec snapshot.
- Continue until all required fields are complete.
- End with explicit approval request in Korean.

Approval prompt format:
`명세가 확정되었습니다. 이 명세 기준으로 구현을 시작해도 될까요? (예/아니오)`

## 5) Mid-Implementation Changes
- If a new unknown or requirement change appears, pause implementation.
- Open a `Spec Change Request` summary:
  - what changed
  - impact on scope/validation/risk
  - additional acceptance criteria
- Resume implementation only after re-approval.

## 6) Source Template
- Use [request-spec-template.md](request-spec-template.md) to draft and track request specs.
