# Production Readiness Gate

## Module freeze
**No new modules** (Purchase, PO, Order Tracking) until:

1. CAM (Central Access) stays green  
2. Product Costing stays green  
3. Vendor Selection stays green  
4. Customer-host production dry-run checklist is signed PASS  

## Current verified baseline (agent / host CI — not customer TLS)

| Area | Evidence |
|------|----------|
| Unit tests | 432 PASS |
| Host Flyway IT | V90 PASS |
| Vitest | 17 PASS |
| Live health | 200 UP |
| Module ACCESS-03 / ACCESS-07 | PASS |
| Playwright | 14 PASS (prior session) |
| prod-smoke.sh | PASS |

## UI polish policy
Polish only against a **concrete defect list** (ticket ID, repro steps, expected vs actual).  
Scan of frontend (V128) found no TODO/FIXME production defects; input `placeholder` attributes are intentional UX.

## Customer dry-run
See `PRODUCTION_DRY_RUN_CHECKLIST.md` — requires real secrets/TLS on customer host.
