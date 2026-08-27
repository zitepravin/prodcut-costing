# FULL_SCALE_QA_REPORT — 2026-08-14 (MASTER V137)

**Source package:** PRODUCT-COSTING-SOFTWARE-MASTER-V136 → V137  
**Flyway latest in repo:** **V91** (payment subscription management)  
**Application package label:** independent of Flyway (master V137)

## Docker environment

| Step | Result | Notes |
|------|--------|-------|
| Docker engine install on agent | PASS | docker 29.1.3 |
| Inspect existing Dockerfile / compose | PASS | Reused `backend/Dockerfile`, extended root `docker-compose.yml` + frontend Dockerfile |
| `docker compose build` | **BLOCKED** | overlayfs whiteout `operation not permitted` on agent host (cannot extract Temurin layers) |
| `docker compose up` full stack | **BLOCKED** | Depends on successful build |
| Docker MySQL-only | **NOT RUN** | Full image pull hit same overlay restriction |

**Deliverable for Windows Docker (user machine):** root `docker-compose.yml`, `backend/Dockerfile`, `frontend/Dockerfile`, `frontend/nginx.conf`, `docs/DOCKER_TEST_STACK.md`

## Host-based execution (MariaDB + Spring Boot + Vite)

| # | Suite | Result | Evidence |
|---|-------|--------|----------|
| 1 | Docker build | **BLOCKED** | overlay whiteout |
| 2 | Docker Compose startup | **BLOCKED** | |
| 3 | Flyway migration (host IT) | **PASS** | FlywayMigrationIntegrationTest → V91 |
| 4 | Backend unit tests | **PASS** | **439** tests, 0 failures |
| 5 | Frontend Vitest | **PASS** | **17** tests |
| 6 | API smoke (`prod-smoke.sh`) | **PASS** | health UP, login, modules, ACCESS-07 → 403 |
| 7 | Central Access (unit) | **PASS** | RequiresModuleAspectTest + CentralAccessServiceTest in unit suite |
| 8 | Tenant isolation ACCESS-07 | **PASS** | prod-smoke |
| 9 | RBAC / module ACCESS-03 | **PASS** | VS disabled → HTTP **403** |
| 10 | Product Costing (unit engines/services) | **PASS** | included in 439 |
| 11 | Vendor Selection (unit + engines) | **PASS** | included in 439 |
| 12 | Negative / validation (unit) | **PASS** | included in 439 |
| 13 | Playwright E2E | **NOT RUN** | Stack not up during PW window; 14 skipped (API/FE unavailable). Prior sessions: 14 PASS on live stack |
| 14 | Full regression (unit+IT+smoke) | **PASS** | unit 439 + Flyway IT 1 + smoke + ACCESS-03 |

## Failures fixed this session
- None in application code. Docker build failure is **environment**, not project Dockerfile logic.

## Production-readiness risks
1. Agent/CI hosts with restricted overlay may not build Temurin images — run Docker on developer Windows/Linux with normal Docker Desktop.
2. Playwright must be re-run when BE+FE are both listening.
3. Customer TLS dry-run still ops-owned.

## Commands for user Windows Docker

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
docker compose up --build -d
curl http://localhost:8080/actuator/health
# Frontend http://localhost:8088
```
