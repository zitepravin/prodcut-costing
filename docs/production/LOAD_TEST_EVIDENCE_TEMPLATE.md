# Load test evidence template

**Script:** `load-tests/k6-multi-tenant-smoke.js`  
**Runbook:** `load-tests/README.md`

| Field | Value |
|-------|-------|
| Date | |
| Environment URL | |
| Operator | |
| TOKEN_A / CUSTOMER_A | (set, not recorded in git) |
| TOKEN_B / CUSTOMER_B | (set, not recorded in git) |
| Peak VUs | |
| Duration | |
| p95 latency (ms) | |
| Error rate | |
| Cross-tenant probe (403/401) | pass / fail |
| Notes | |

```bash
export BASE_URL=...
export TOKEN_A=... TOKEN_B=...
export CUSTOMER_A=... CUSTOMER_B=...
k6 run load-tests/k6-multi-tenant-smoke.js
```

Attach k6 summary JSON to the change ticket. Update `PRODUCTION_READINESS_GATE.md` when complete.
