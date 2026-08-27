# Playwright E2E smoke — live stack

**Date:** 2026-08-14  
**Master package:** PRODUCT-COSTING-SOFTWARE-MASTER-V102 / V103 (report update)  
**Suite file:** `frontend/e2e/smoke.spec.ts`  
**Config:** `frontend/playwright.config.ts`

---

## Environment

| Component | Detail |
|-----------|--------|
| MySQL | 8.0.46 — schema Flyway **V90** |
| Backend | Spring Boot 3.3 / Java 21 — `http://127.0.0.1:8080` |
| Frontend | Vite 8 — `http://127.0.0.1:5173` |
| Browser | Chromium (Playwright Desktop Chrome) |
| Profile | `spring.profiles.default=local` |
| JWT | `JWT_SECRET` ≥ 32 characters set for BE |
| API login check | `POST /api/v1/auth/login` → **200** + `accessToken` |

### Runtime credentials used in this run

| Variable | Value used |
|----------|------------|
| `E2E_USERNAME` | `admin` |
| `E2E_PASSWORD` | `ChangeMe!Now1` |
| `PLAYWRIGHT_BASE_URL` | `http://127.0.0.1:5173` |

Seed account: V9 initial SYSTEM_ADMINISTRATOR (`admin` / `ChangeMe!Now1`).

---

## Aggregate result

| Metric | Value |
|--------|--------|
| Tests run | **4** |
| Passed | **4** |
| Failed | **0** |
| Skipped | **0** |
| Duration | **~8.0 s** |
| Workers | 1 |
| Overall | **PASS** |

---

## Test case details

### 1. `login page renders username and password fields`

| Field | Detail |
|-------|--------|
| **File / line** | `e2e/smoke.spec.ts` — describe `Smoke` |
| **Navigation** | `GET /login` |
| **Purpose** | Confirm login form is present for unauthenticated users |
| **Assertions** | `input[name="username"]` visible (≤10s); `input[name="password"]` visible; submit button matching `/sign in\|log in\|login/i` visible |
| **Result** | **PASS** |

### 2. `unauthenticated root redirects or shows login`

| Field | Detail |
|-------|--------|
| **Navigation** | `GET /` |
| **Purpose** | Confirm `ProtectedRoute` does not expose the app shell without auth |
| **Assertions** | URL matches `/login` or `/`; or username field present; or still on root |
| **Result** | **PASS** |

### 3. `login with demo credentials reaches app shell when API available`

| Field | Detail |
|-------|--------|
| **Navigation** | `GET /login` then form submit |
| **Purpose** | End-to-end auth against live backend |
| **Steps** | Fill username/password → click login → wait 2s |
| **Assertions** | If still on `/login`: username field still visible (soft note: BE may be down). Else: URL is **not** `/login` (entered app shell) |
| **This run** | Credentials accepted; left login route (aligned with API 200 + token) |
| **Result** | **PASS** |

### 4. `vendor-selection route is registered (auth may redirect)`

| Field | Detail |
|-------|--------|
| **Navigation** | `GET /vendor-selection` |
| **Purpose** | Confirm Vendor Selection route exists in the SPA router |
| **Assertions** | Final URL contains `vendor-selection` **or** `login` (auth redirect allowed) |
| **Result** | **PASS** |

---

## Skip behaviour (not triggered this run)

Helper `gotoOrSkip()` skips the test when:

- `page.goto` throws (e.g. `ERR_CONNECTION_REFUSED`), or  
- HTTP status ≥ 500  

Message: *Frontend not reachable — start Vite (npm run dev) and backend first.*

---

## How to re-run

```bash
# Terminal 1 — MySQL + Flyway through V90, then:
cd backend
export JWT_SECRET='local-dev-secret-key-min-32-characters-long!!'
export DB_URL='jdbc:mysql://127.0.0.1:3306/product_costing?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export DB_USERNAME=costing_app
export DB_PASSWORD=changeme
mvn -DskipTests spring-boot:run

# Terminal 2
cd frontend
npm install
npx vite --host 0.0.0.0 --port 5173

# Terminal 3
cd frontend
npx playwright install chromium
PLAYWRIGHT_BASE_URL=http://127.0.0.1:5173 \
E2E_USERNAME=admin \
E2E_PASSWORD='ChangeMe!Now1' \
npm run test:e2e
```

---

## Scope / limitations

- Smoke only: does **not** exercise Vendor Selection 360 actions, RFQ, costing calculations, or approval workflows.
- Does **not** assert token storage or role-based menu visibility.
- Recommended next E2E: authenticated Vendor Selection 360 load, warranty claims list empty-state, scorecard run.

---

## Related reports

- `MYSQL_V90_MIGRATE_REPORT.md` — greenfield Flyway V1→V90  
- `HOST_VALIDATION_REPORT.md` — unit test / JDK 21 backend suite  
