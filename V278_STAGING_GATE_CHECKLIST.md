# V278 Staging Gate Checklist

Use package: `PRODUCT-COSTING-SOFTWARE-MASTER-V278.zip`

## A. Deploy
- [ ] Deploy backend + frontend from V278 zip
- [ ] Set `SPRING_PROFILES_ACTIVE=prod`
- [ ] Strong `JWT_SECRET`, `CORS_ORIGINS`, Redis, MySQL SSL as required by `application-prod.yml`

## B. Database
- [ ] Flyway runs through **V148**
- [ ] Confirm `actual_cost_record.customer_id` NOT NULL + indexes
- [ ] Smoke: create tenant, seed membership

## C. Dual-tenant security
- [ ] Tenant A imports actual cost on BOM-A
- [ ] Tenant B cannot GET `/api/v1/financial/actual-costs?bomNodeId=BOM-A` (404/403, no data)
- [ ] Tenant B cannot import against Tenant A BOM
- [ ] Repeat for Should-Cost generate and BOM node get

## D. Feature entitlement
- [ ] Disable feature `ACTUAL_COST` for tenant → import returns **403**
- [ ] Disable `SHOULD_COST_CALCULATE` → generate returns **403**
- [ ] Disable `FX_HISTORICAL` → currency convert returns **403**
- [ ] Module PRODUCT_COSTING OFF → module gate denies first

## E. Freeze
- [ ] Generate should-cost at USD→INR 84.50, approve/lock
- [ ] Change master rate to 86.00
- [ ] Re-read approved build-up: FX snapshot still 84.50

## F. Load (k6)
```bash
export BASE_URL=... TOKEN_A=... TOKEN_B=... CUSTOMER_A=... CUSTOMER_B=...
k6 run load-tests/k6-multi-tenant-smoke.js
```
- [ ] Record p95, error rate, Hikari active/pending, DB connections

## G. DR
- [ ] Backup MySQL
- [ ] Restore to clean instance
- [ ] App boots; tenant data present; smoke API OK

## Pass criteria
All boxes checked with evidence (logs/screenshots/metrics). Until then: **PRODUCTION READY WITH CONDITIONS**.
