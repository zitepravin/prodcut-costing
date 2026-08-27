# SESSION V196 — Should-Cost generate 500 diagnostics + harden

## Root cause analysis
- GlobalExceptionHandler.handleUnexpected **did not log** the exception → docker logs showed no stack for HTTP 500.
- `isAnyCalculationStale` could NPE if a material/process/tooling calculation row had null `calculated_at`.

## Fixes
1. Log unexpected exceptions at ERROR with URI + stack.
2. Null-safe `calculatedAt` checks in should-cost staleness.
3. Optional SQL: `backend/scripts/seed_sga_stamping.sql` for STAMPING / NA benchmarks.

## Customer verify
```text
docker compose build backend --no-cache
docker compose up -d backend
# seed benchmarks if empty
docker exec -i pcs-mysql mysql -uroot -prootpass product_costing < backend/scripts/seed_sga_stamping.sql
# generate as demo.engineer
```
