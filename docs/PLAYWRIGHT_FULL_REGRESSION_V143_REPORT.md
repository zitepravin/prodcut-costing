# Full Playwright Regression — MASTER V143 (2026-08-14)

## Stack
- Host MariaDB + Spring Boot local (Flyway V91) :8080
- Vite :5173

## Results (final verified)

| Suite | Result | Notes |
|-------|--------|-------|
| helping-agent.spec.ts | **3 PASS** | API + FAB UI |
| module-entitlement.spec.ts | **2 PASS** | ACCESS-07 + ACCESS-03 |
| smoke.spec.ts | **4 PASS** | |
| vendor-selection.spec.ts | **4 PASS** | |
| vendor-selection-decision.spec.ts | **2 PASS** | |
| vendor-selection-360.spec.ts | **1 PASS / 1 intermittent** | list OK; ACME-BRG load depends on UI tenant context + empty 360 payload messaging |

**Confirmed green this session: 16 PASS** (ACCESS-03 fixed with dedicated NOVS tenant).  
**360 seed load:** intermittent when UI session lacks selected customer or 360 returns empty sections without “Supplier 360 loaded” toast timing.

## ACCESS-03 setup (required for entitlement test)
```
Customer NOVS + entitlement VENDOR_SELECTION enabled=false
+ PRODUCT_COSTING enabled=true
+ active membership for admin
E2E_CUSTOMER_ID=<novs-customer-id>
```
DEFAULT tenant has VS **enabled** — cannot satisfy ACCESS-03.

## Not claimed
- Docker compose PASS
- Customer production dry-run PASS
