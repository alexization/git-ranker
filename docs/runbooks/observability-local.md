# Observability Runbook (Local)

## Goal
Give humans and AI agents a repeatable path for debugging with logs and metrics.

## Local Stack
- Application actuator endpoint: `http://localhost:9090/actuator`
- Prometheus: `http://localhost:9091`
- Grafana: `http://localhost:3001`
- Loki: `http://localhost:3100`

## Startup
1. Export required env values in `.env`.
2. Start stack:
`docker compose up -d`
3. Verify health:
`curl -s http://localhost:9090/actuator/health`
4. Verify metrics:
`curl -s http://localhost:9090/actuator/prometheus | head`

## Minimum Checks Per Change
1. Endpoint change:
- Confirm HTTP status and response shape.
- Confirm request appears in metrics (`http_server_requests`).

2. Batch change:
- Confirm batch logs include target identifiers.
- Confirm failure counters/timers are visible.

3. Error handling change:
- Confirm mapped error response from `GlobalExceptionHandler`.
- Confirm logs capture error type without secrets.

## Investigation Loop
1. Reproduce issue with precise input.
2. Correlate timestamp with application logs.
3. Check relevant metric trend during the same window.
4. Form hypothesis and retest.
5. Record finding in plan doc.

## Notes
- Never log tokens, secrets, or raw credentials.
- Do not log raw usernames by default. Prefer hashed or opaque identifiers for traceability.
- Acceptable examples: `hashUsername(username)`, `maskUsername(username)`, opaque `nodeId`, internal session correlation key.
- Raw username logging is allowed only when explicitly authorized by data-classification policy and approved by privacy/compliance.
- Keep metric definitions in sync with [../observability/metrics-catalog.md](../observability/metrics-catalog.md).
- Keep structured log fields in sync with [../observability/logging-contract.md](../observability/logging-contract.md).
