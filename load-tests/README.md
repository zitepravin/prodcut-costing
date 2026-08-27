# Load tests

## Multi-tenant smoke

```bash
export BASE_URL=https://staging.example.com
export TOKEN_A=...   # JWT for user in customer A
export TOKEN_B=...   # JWT for user in customer B
export CUSTOMER_A=... # platform customer UUID A
export CUSTOMER_B=... # platform customer UUID B

k6 run k6-multi-tenant-smoke.js
```

### What it covers

- Health check
- Alternating traffic between two tenants
- Thresholds: p95 &lt; 800ms, error rate &lt; 5% (tighten after optimization)

### Cross-tenant check (manual / extend script)

After creating a `vehicle_program` under customer A, call GET with `TOKEN_B` + `CUSTOMER_B` — expect **404/403**, never the A payload.

### Evidence to record

| Field | Value |
|-------|-------|
| Date | |
| Environment | |
| Peak VUs | |
| p95 latency | |
| Error rate | |
| Cross-tenant leaks observed | yes/no |
| Operator | |

Attach k6 summary JSON to the change ticket.
