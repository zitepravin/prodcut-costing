# Production Hardening Notes (V253+)

## Connection Pool Formula

```
Hikari maximum-pool-size ≈ (MySQL max_connections − reserve) / max_backend_instances
```

- Reserve admin + monitoring + replica threads (typically 20–50).
- Prefer smaller pools and faster queries over large pools.
- Expose Hikari metrics via Actuator `/actuator/metrics/hikaricp.*` and Prometheus.
- Override at runtime: `HIKARI_MAX_POOL`, `HIKARI_MIN_IDLE`.

## Correlation ID

- Filter: `CorrelationIdFilter`
- Headers: `X-Request-Id`, `X-Correlation-Id`
- MDC key: `correlationId`
- Appears in log pattern and email/outbox rows when set.

## Transactional Outbox

- Table: `domain_outbox` (Flyway V130)
- Service: `DomainOutboxService.enqueue(...)`
- Claim pattern uses `FOR UPDATE SKIP LOCKED` for multi-instance workers.
- Wire business methods to enqueue after aggregate persist (same TX).

## File Metadata

- Table: `stored_file` (Flyway V130)
- Object storage holds bytes; MySQL holds metadata only.
- Always set `customer_id` (NOT NULL).

## Multi-Tenancy Status

- Access plane: `X-Customer-Id` + membership + module entitlements — implemented.
- Data plane: **incomplete** — core domain tables lack `customer_id`.
- Do not claim 1,000-tenant isolation until data-plane migration and tests are complete.

## Rate Limiting

- Login: in-memory only (not multi-instance safe).
- General API / tenant quotas: require Redis or API gateway (P0 remaining).

## Observability

- Actuator: health, info, metrics, prometheus (configurable via `ACTUATOR_EXPOSE`).
- Protect `/actuator` in production with network policy or authentication.

## Load Testing

- Script: `load-tests/k6-multi-tenant-smoke.js`
- Run against staging that matches production topology before go-live.
- Do not claim p95 targets without measured results.
