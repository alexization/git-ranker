# Docs Index

This directory is the project system of record.
If code and docs disagree, update docs in the same PR.

## Core Maps
- Harness roadmap: [harness/roadmap.md](harness/roadmap.md)
- Issue/PR playbook: [harness/issue-pr-playbook.md](harness/issue-pr-playbook.md)
- Layering rules: [architecture/layering.md](architecture/layering.md)
- Testing runbook: [runbooks/testing.md](runbooks/testing.md)
- Observability runbook: [runbooks/observability-local.md](runbooks/observability-local.md)
- Metrics catalog: [observability/metrics-catalog.md](observability/metrics-catalog.md)

## Execution Documents
- Plan template: [plans/TEMPLATE.md](plans/TEMPLATE.md)
- Active plans: [plans/active/README.md](plans/active/README.md)
- Completed plans: [plans/completed/README.md](plans/completed/README.md)
- ADR template: [adr/ADR-TEMPLATE.md](adr/ADR-TEMPLATE.md)
- ADR index: [adr/README.md](adr/README.md)

## Documentation Rules
1. Non-trivial changes require a plan doc under `docs/plans/active/`.
2. Architecture-impacting changes require ADR update.
3. PR must include document links and validation evidence.
4. After merge, move plan doc to `docs/plans/completed/`.
