# Plan: 2026-02-25-m4-3-weekly-scorecard

## 1) Purpose
- Problem: quality reviews are ad hoc and hard to compare week over week.
- Intended outcome: establish a weekly scorecard template for measurable drift review.
- Non-goals: automate metric extraction or implement reporting dashboards in this step.

## 2) Scope
- In scope:
  - add scorecard documentation structure
  - add reusable weekly scorecard template
  - add issue template for weekly scorecard creation
  - link scorecard docs from system-of-record index
- Out of scope:
  - scheduled issue auto-creation workflow
  - CI gate based on scorecard values

## 3) Progress
- [x] add `docs/scorecards/README.md`
- [x] add `docs/scorecards/weekly-template.md`
- [x] add `.github/ISSUE_TEMPLATE/weekly_scorecard.yml`
- [x] update docs index and AGENTS references

## 4) Design Notes
- Constraints: keep template compact enough for weekly adoption.
- Tradeoffs: manual data entry first, automation later after fields stabilize.
- Open questions: when to enforce scorecard completion as a release gate.

## 5) Decision Log
- 2026-02-25: include delivery, reliability, architecture, observability, and documentation drift in one template.
- 2026-02-25: create weekly issue template to standardize review kickoff.

## 6) Validation Plan
- Required commands:
  - YAML validation for issue template files
- Observability checks:
  - not applicable (docs/process update)

## 7) Risks and Rollback
- Risks: process adoption may be inconsistent in early weeks.
- Rollback strategy: simplify template sections and focus on top three metrics only.

## 8) Result Summary
- What changed: weekly scorecard process and template were added.
- Test result summary:
  - `.github/**/*.yml` validation passed (`yaml-ok`)
- Follow-up tasks:
  - evaluate adding scheduled issue creation in GitHub Actions
