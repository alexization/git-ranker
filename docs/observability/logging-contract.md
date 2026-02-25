# Logging Field Contract (M2-3)

## 1) Purpose
- Standardize required fields so humans and agents interpret logs consistently.
- Correlate HTTP, batch, and external API flows using `trace_id`.

## 2) Scope
- Applies to structured logs emitted through `LogContext.event(...)`.
- Plain `log.debug(...)` calls should be migrated to `LogContext` incrementally.

## 3) Required Fields
| Field | Description | Source |
| --- | --- | --- |
| `trace_id` | Correlation identifier for request/job flow | Set in request filter, auto-generated if missing |
| `event` | Event name (enum) | Auto-set from `Event` |
| `log_category` | Event category | Auto-set from `Event.Category` |
| `phase` | Flow phase | Default: lowercase category, can be overridden |
| `outcome` | Result status | Default: INFO/DEBUG=`success`, WARN=`warning`, ERROR=`failure` |

## 4) Request Context Fields
| Field | Description |
| --- | --- |
| `client_ip` | Client IP |
| `user_agent` | User agent |
| `request_method` | HTTP method |
| `request_uri` | Request URI |
| `username` | Authenticated username (when available) |

## 5) Recommended Fields by Domain
1. HTTP response
- `status`, `latency_ms`

2. GitHub API
- `operation`, `target`, `latency_ms`, `error_type`, `status`, `remaining`, `cost`

3. Batch
- `job_name`, `total_count`, `success_count`, `fail_count`, `skip_count`, `duration_ms`

4. Error handling
- `error_code`, `error_status`, `error_type`, `error_message`

## 6) Prohibitions and Cautions
1. Do not log secrets (tokens, passwords, raw auth headers).
2. Do not log full payload bodies unless sampled/redacted.
3. Mask messages that may include personal data.

## 7) Verification Criteria
1. Structured logging tests must assert required field presence.
2. PRs must keep this contract in sync with changed logging fields.
