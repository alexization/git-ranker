# Weekly Scorecards

This directory stores weekly quality and drift reviews for the harness workflow.

## Purpose
- Track whether delivery speed, reliability, and architecture quality are improving.
- Detect documentation, test, and observability drift before it becomes costly.
- Convert review findings into next-week action items.

## Workflow
1. Create a weekly issue from `.github/ISSUE_TEMPLATE/weekly_scorecard.yml`.
2. Create a scorecard file from `weekly-template.md`.
3. Fill metrics and findings at the end of the week.
4. Link merged PRs, incidents, and active plans.
5. Add follow-up tasks and owners for the next week.

## Naming Convention
- `YYYY-Www-scorecard.md`
- Example: `2026-W09-scorecard.md`

## Required Links
- Related weekly issue
- PRs merged in the week
- Incidents/postmortems (if any)
- Updated plans/ADRs
