# Docs Index

This directory is the project system of record.
If code and docs disagree, update docs in the same PR.

## Core Maps
- Harness roadmap: [harness/roadmap.md](harness/roadmap.md)
- Issue/PR playbook: [harness/issue-pr-playbook.md](harness/issue-pr-playbook.md)
- Spec gate policy: [harness/spec-gate.md](harness/spec-gate.md)
- AI review loop: [harness/ai-review-loop.md](harness/ai-review-loop.md)
- Request spec template: [harness/request-spec-template.md](harness/request-spec-template.md)
- Layering rules: [architecture/layering.md](architecture/layering.md)
- Testing runbook: [runbooks/testing.md](runbooks/testing.md)
- Observability runbook: [runbooks/observability-local.md](runbooks/observability-local.md)
- Metrics catalog: [observability/metrics-catalog.md](observability/metrics-catalog.md)
- Logging contract: [observability/logging-contract.md](observability/logging-contract.md)
- Scorecards guide: [scorecards/README.md](scorecards/README.md)
- Weekly scorecard template: [scorecards/weekly-template.md](scorecards/weekly-template.md)

## Execution Documents
- Plan template: [plans/TEMPLATE.md](plans/TEMPLATE.md)
- Active plans: [plans/active/README.md](plans/active/README.md)
- Completed plans: [plans/completed/README.md](plans/completed/README.md)
- ADR template: [adr/ADR-TEMPLATE.md](adr/ADR-TEMPLATE.md)
- ADR index: [adr/README.md](adr/README.md)

## Documentation Rules
1. Implementation starts only after spec gate approval.
2. Non-trivial changes require a plan doc under `docs/plans/active/`.
3. Architecture-impacting changes require ADR update.
4. PR must include document links and validation evidence.
5. Practical mode only: move plan doc to `docs/plans/completed/` in the same PR before final merge.
6. Codex and CodeRabbitAI review loop must be completed before merge.
7. Machine-read policy docs (`AGENTS.md`, `docs/*`) must be written in English.
