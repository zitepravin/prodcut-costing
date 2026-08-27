# Production Dry-Run Checklist (Customer Host)

**Purpose:** Execute on the customer host with real TLS, secrets, and network.  
**Agent sandbox cannot complete this** — no customer TLS certificates or production secrets.  
**Related:** `PRODUCTION_DEPLOYMENT_RUNBOOK.md`, `PRODUCTION_SECURITY_CHECKLIST.md`, `scripts/prod-smoke.sh`

**Seed defaults to retire immediately**
| Item | Seed (must not ship) |
|------|----------------------|
| Admin user | `admin` |
| Admin password | `ChangeMe!Now1` |
| DB password | `changeme` |
| JWT | any `CHANGE_ME` / short secret |

---

## 1. Pre-flight

- [ ] JDK **21+** with `javac` (Temurin recommended) on app host
- [ ] Maven **3.9+** (or pre-built jar from CI)
- [ ] Node **20+** if building frontend on host (`npm ci && npm run build`)
- [ ] MySQL **8** or MariaDB **10.11+**, UTF8MB4, reachable on **private** network only
- [ ] TLS certificate for public API/SPA hostname (reverse proxy / LB)
- [ ] Secrets via vault / orchestrator env — **not** shell history or committed files
- [ ] DB backup taken **before** first Flyway migrate on this environment
- [ ] NTP / time sync enabled (JWT validation depends on clock skew)

## 2. Secrets & profile (must not be defaults)

- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] `JWT_SECRET` ≥ 32 chars, unique, non-placeholder (startup fails if weak when require-strong is on)
- [ ] `REQUIRE_STRONG_JWT_SECRET=true` (prod default)
- [ ] `DB_URL` points at production DB (prefer TLS: `useSSL=true` / platform equivalent)
- [ ] `DB_USERNAME` / `DB_PASSWORD` — strong; **not** seed `changeme`
- [ ] `CORS_ORIGINS` = exact SPA origin(s), comma-separated — **not** `*`
- [ ] `SWAGGER_PUBLIC=false`
- [ ] Admin password **rotated** from `ChangeMe!Now1`
- [ ] Optional: MFA enabled for `SYSTEM_ADMINISTRATOR` / `ADMIN` accounts
- [ ] Optional SMTP only if `NOTIFICATION_EMAIL_ENABLED=true` (mail health stays off unless configured)

## 3. Deploy

- [ ] Schema empty or known baseline; Flyway enabled
- [ ] After start: `flyway_schema_history` max version = **91**, `success = 1`
- [ ] `spring.jpa.hibernate.ddl-auto=validate` (prod profile — no DDL from Hibernate)
- [ ] Backend process under systemd / k8s / equivalent with prod env only
- [ ] Frontend `dist/` served over **HTTPS**
- [ ] Reverse proxy: `/api` → backend; `X-Forwarded-Proto` / `X-Forwarded-For` set
- [ ] Actuator exposure limited to **health** (and info if required) — not `env` / `beans` / `heapdump`
- [ ] App log level **INFO** (not DEBUG) for `com.automotive.costing` in prod
- [ ] Note: `management.health.mail.enabled=false` by default so missing SMTP does not mark DOWN

## 4. Automated smoke (ops workstation)

```bash
export BASE=https://api.customer.example   # HTTPS
export USER=admin
export PASS='<rotated-admin-password>'
./scripts/prod-smoke.sh
```

- [ ] Script exits 0
- [ ] `/actuator/health` → **200** and `"status":"UP"` (db component UP)
- [ ] `POST /api/v1/auth/login` → **200** + accessToken
- [ ] `GET /api/v1/platform/modules` (Bearer) → **200**
- [ ] VS 360 **without** `X-Customer-Id` → **403** ACCESS-07 / TENANT_ACCESS_DENIED

## 5. Manual security / CAM checks

- [ ] Swagger UI (`/swagger-ui.html`) **not** publicly usable (`SWAGGER_PUBLIC=false`)
- [ ] Unauthenticated `/api/v1/platform/customers` → **401**
- [ ] With admin JWT + `X-Customer-Id` for ACTIVE tenant:
  - [ ] `GET .../vendor-selection/suppliers/{id}/360` → **200** or **404** (not 403) when VS entitlement ON
  - [ ] `GET .../master/materials?page=0&size=1` → **200** when PC entitlement ON
- [ ] Disable `VENDOR_SELECTION` entitlement for test tenant → VS API → **403** ACCESS-03 / MODULE_NOT_ENABLED → re-enable
- [ ] UI: login, tenant selector, Product Costing nav, Vendor Selection nav (only if entitled)
- [ ] Confirm FE is not treated as security boundary (deny by removing entitlement, not by hiding menu alone)

## 6. Central Access data

- [ ] Tenant (DEFAULT or customer) **ACTIVE**
- [ ] Modules `PRODUCT_COSTING` + `VENDOR_SELECTION` entitled **ON** for that tenant
- [ ] Operator user memberships **ACTIVE**
- [ ] UI tenant selector (`pcs.activeCustomerId`) matches intended tenant

## 7. Rollback readiness

- [ ] Previous jar + frontend `dist` artifact retained
- [ ] DB backup restore path tested or documented (RPO/RTO agreed)
- [ ] No reliance on Flyway “down” scripts (forward-fix only unless approved)

## 8. Sign-off

| Role | Name | Date | Result | Notes |
|------|------|------|--------|-------|
| Ops | | | PASS / FAIL | |
| Security | | | PASS / FAIL | |
| Product | | | PASS / FAIL | |

**Gate:** Do not open Purchase / PO / Order modules until this checklist is **PASS** and CAM / Product Costing / Vendor Selection remain green.

---

## Review notes (for maintainers)

| Strength | Gap closed in this revision |
|----------|-----------------------------|
| Clear secrets + smoke + CAM | DB_URL / DDL validate / backup before migrate |
| ACCESS-07 automated | ACCESS-03 + PC path + swagger closedown |
| Sign-off table | Rollback + log level + actuator surface |

---

## 8. Helping Agent (Product Costing, read-only) — if entitled

- [ ] `POST /api/v1/helping-agent/chat` without JWT → **401/403**
- [ ] With JWT but **without** `X-Customer-Id` → **403** ACCESS-07
- [ ] With JWT + `X-Customer-Id` (PRODUCT_COSTING enabled) → **200**, body has `classification` in FACT/CALCULATED/RULE_RESULT/AI_INFERENCE/MISSING_DATA and `correlationId`
- [ ] Optional: `./scripts/helping-agent-smoke.sh` with `BASE` / `CUSTOMER_ID` exits 0
- [ ] Confirm agent cannot write (no BOM/approval mutate endpoints invoked from tools)

---

## 9. Sign-off

| Role | Name | Date | Result (PASS/FAIL) |
|------|------|------|--------------------|
| Ops | | | |
| Customer | | | |

**Notes / defects:**
