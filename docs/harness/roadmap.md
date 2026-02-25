# Harness Roadmap (Issue -> Commit -> PR)

This roadmap applies the OpenAI harness sequence to `git-ranker`.
Execution unit is always small, reviewable PRs with explicit intent and evidence.

## Phase 1: Increasing Application Legibility

### Epic Issue
- Title: `Harness M1 - Increase Application Legibility`
- Goal: make repository intent and boundaries obvious to humans and AI agents.

### Child Issues and PR Plan
1. Issue: `M1-1 Add AGENTS entrypoint and docs index`
- PR: `docs: add AGENTS.md and system-of-record docs index`
- Suggested commits:
  - `docs(agents): add root AGENTS execution contract`
  - `docs(index): establish system-of-record map`

2. Issue: `M1-2 Define layering and test runbook`
- PR: `docs: add architecture boundary and testing runbook`
- Suggested commits:
  - `docs(architecture): define package dependency direction`
  - `docs(runbook): add testing command matrix`

3. Issue: `M1-3 Upgrade issue/pr templates for intent spec`
- PR: `chore(github): enforce intent and validation in templates`
- Suggested commits:
  - `chore(template): add harness epic/task issue forms`
  - `chore(template): update PR template with plan/docs/validation sections`

## Phase 2: Giving Codex a Full Observability Stack

### Epic Issue
- Title: `Harness M2 - Full Observability Stack for Agent Work`
- Goal: allow agent workflows to verify behavior through logs/metrics dashboards.

### Child Issues and PR Plan
1. Issue: `M2-1 Local observability runbook and query checklist`
- PR: `docs(observability): add local runbook for log+metric debugging`
- Suggested commits:
  - `docs(runbook): add observability-local workflow`

2. Issue: `M2-2 Metrics catalog and SLI targets`
- PR: `docs(observability): define SLI/SLO-oriented metrics catalog`
- Suggested commits:
  - `docs(metrics): map business and system metrics to signals`

3. Issue: `M2-3 Structured log field contract`
- PR: `feat(logging): standardize required fields for key flows`
- Suggested commits:
  - `refactor(logging): align key-value fields across filters/aspects`
  - `test(logging): add contract tests for mandatory fields`

## Phase 3: System of Record

### Epic Issue
- Title: `Harness M3 - Establish Docs as System of Record`
- Goal: require plans and decisions to live in repository docs and stay synced.

### Child Issues and PR Plan
1. Issue: `M3-1 Add execution plan workflow`
- PR: `docs(plan): add active/completed plan lifecycle`
- Suggested commits:
  - `docs(plan): add template and lifecycle directories`

2. Issue: `M3-2 Introduce ADR workflow`
- PR: `docs(adr): add ADR template and index`
- Suggested commits:
  - `docs(adr): add decision template`
  - `docs(adr): add write conditions and naming rules`

3. Issue: `M3-3 Migrate existing major changes into plan+ADR format`
- PR: `docs(migration): backfill plan/adrs for recent critical decisions`
- Suggested commits:
  - `docs(migration): add backfilled records`

## Phase 4: Feedback Loop and Guardrails

### Epic Issue
- Title: `Harness M4 - Build Continuous Feedback Loop`
- Goal: convert workflow rules into CI-enforced checks and architecture guardrails.

### Child Issues and PR Plan
1. Issue: `M4-1 PR contract check in CI`
- PR: `ci: validate plan link and validation evidence in PR body`
- Suggested commits:
  - `ci(guardrail): add pr body contract workflow`

2. Issue: `M4-2 Add architecture tests`
- PR: `test(architecture): enforce layer boundaries via ArchUnit`
- Suggested commits:
  - `build(test): add archunit dependency`
  - `test(arch): add initial layer constraint tests`

3. Issue: `M4-3 Weekly scorecard and drift review`
- PR: `docs(scorecard): add weekly quality and drift checklist`
- Suggested commits:
  - `docs(scorecard): add measurement template`

## Phase 5: Quality Automation and Runtime Safety

### Epic Issue
- Title: `Harness M5 - Raise Verification Confidence`
- Goal: turn quality assumptions into automated checks for coverage, runtime safety, and PII-safe logging.

### Child Issues and PR Plan
1. Issue: `M5-1 Add coverage quality gate`
- PR: `ci(quality): enforce baseline unit-test coverage with report artifacts`
- Suggested commits:
  - `build(quality): add jacoco coverage verification tasks`
  - `ci(quality): add pull-request quality gate workflow`

2. Issue: `M5-2 Harden deployment health checks`
- PR: `ci(deploy): require strict health status after deployment`
- Suggested commits:
  - `ci(deploy): require HTTP 200 and UP status for health checks`
  - `config(prod): reduce actuator endpoint exposure`

3. Issue: `M5-3 Enforce PII-safe logging defaults`
- PR: `refactor(logging): mask username fields by default in log context`
- Suggested commits:
  - `refactor(logging): add username masking sanitizer`
  - `test(logging): add masking behavior tests`

4. Issue: `M5-4 Enforce spec approval gate and Korean clarification loop`
- PR: `docs(harness): enforce pre-implementation spec gate with Korean clarification loop`
- Suggested commits:
  - `docs(harness): add spec gate policy and request template`
  - `docs(agents): require approved spec before implementation`

## Operating Rules
1. Every child issue must map to one primary PR.
2. PRs should target one outcome and avoid mixed concerns.
3. Merge only with intent, evidence, and docs alignment.
