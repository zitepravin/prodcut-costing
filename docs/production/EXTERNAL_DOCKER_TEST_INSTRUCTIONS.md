# External Docker testing instructions (post-sandbox)

**Sandbox rule:** Docker was NOT available and was NOT executed.  
Run the following only on a machine with Docker Engine + Compose.

## 1. Local stack smoke (dev credentials OK)

```bash
cd PRODUCT-COSTING-SOFTWARE
docker compose up --build -d
# wait for backend health
curl -sf http://localhost:8080/actuator/health
# login + set X-Customer-Id on subsequent calls
```

## 2. Production-shaped stack (secrets required)

```bash
cp docker-compose.prod.example.yml docker-compose.prod.yml
# export JWT_SECRET DB_* REDIS_* CORS_ORIGINS S3_* MYSQL_ROOT_PASSWORD
docker compose -f docker-compose.prod.yml up --build -d
```

Confirm backend **fails** if `RATE_LIMIT_STORE=memory` under `prod` (ProductionHardeningValidator).

## 3. Cross-tenant live HTTP

```bash
# Obtain TOKEN_A / TOKEN_B and CUSTOMER_A / CUSTOMER_B
curl -H "Authorization: Bearer $TOKEN_A" -H "X-Customer-Id: $CUSTOMER_B" \
  http://localhost:8080/api/v1/vehicle-programs/...
# Expect 403/404, never Customer B payload
```

## 4. k6 multi-tenant

```bash
export BASE_URL=http://localhost:8080
export TOKEN_A=... TOKEN_B=... CUSTOMER_A=... CUSTOMER_B=...
k6 run load-tests/k6-multi-tenant-smoke.js
```

## 5. Playwright E2E

```bash
# against frontend :8088
npx playwright test
```

## 6. DR (MySQL dump/restore)

Follow `docs/production/DISASTER_RECOVERY_RUNBOOK.md` and fill `DR_DRILL_EVIDENCE_TEMPLATE.md`.

Record all results in `docs/production/LOAD_TEST_EVIDENCE_TEMPLATE.md` and readiness report.
