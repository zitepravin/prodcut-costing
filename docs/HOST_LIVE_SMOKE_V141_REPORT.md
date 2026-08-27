# Host Live Smoke Report — MASTER V141 (2026-08-14)

## Environment
- MariaDB 10.11 on agent host (127.0.0.1:3306)
- Spring Boot local profile, JWT strength check off
- Flyway applied to **V91** on `product_costing`

## Results

| Check | Result |
|-------|--------|
| Actuator health | **200 UP** (db UP) |
| Login admin | **200** accessToken issued |
| prod-smoke.sh | **PASS** (health, login, modules, ACCESS-07) |
| Helping Agent anonymous | **403** (expected) |
| Helping Agent without X-Customer-Id | **403 ACCESS-07** |
| Helping Agent chat + DEFAULT customer | **200** classification=FACT, sources=1 |
| FlywayMigrationIntegrationTest | **PASS** (V91 on product_costing_it) |

## Notes
- Seed customer `DEFAULT` (`ae080cc8-97f4-11f1-b526-02163e000001`) has PRODUCT_COSTING enabled
- `scripts/helping-agent-smoke.sh` updated to assert ACCESS-07 and auto-discover/use CUSTOMER_ID
- Frontend Playwright UI not run (Vite not started this session)
- Docker compose still not validated on this host
