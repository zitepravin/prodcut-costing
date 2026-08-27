# Host Validation Report — MASTER V95 codebase

**Date:** 2026-08-14  
**Environment:** Sandbox with full Temurin JDK 21.0.12 + Apache Maven 3.9.6

## 1. Backend — `mvn clean test`

| Metric | Result |
|--------|--------|
| JDK | Eclipse Temurin **21.0.12** (`javac` 21.0.12) |
| Maven | **3.9.6** |
| Command | `mvn -B clean test` |
| **Tests run** | **424** |
| **Failures** | **0** |
| **Errors** | **0** |
| **Skipped** | **1** |
| **Build** | **SUCCESS** (~56 s) |
| JaCoCo | Report generated (527 classes analyzed) |

Notable: `CostVersionServiceTest` — 5 tests, 0 failures (includes BOM auto-snapshot case).

### Limitation
- Local MySQL integration ITs (FlywayFullMigrationLocalIT) depend on a running MySQL instance; this run used the default unit/mock test profile. Prior session documented Flyway through V89 PASS on local MySQL 8 when DB is available.

## 2. Playwright E2E

| Check | Result |
|-------|--------|
| `playwright` dependency in frontend | **Not present** |
| `playwright.config.*` | **Not present** |
| `e2e/` or `*.spec.ts` UI tests | **Not present** |
| Execution | **Not runnable** — no suite in repository |

**Required on a true host to enable Playwright:**
1. Add `@playwright/test` + config
2. Write smoke specs (login, dashboard, vendor selection 360)
3. Run backend + frontend against MySQL
4. `npx playwright install` + `npx playwright test`

## 3. Frontend unit tests (prior / this tree)

Vitest: 17/17 PASS (money, evidenceStatus, auditAction, getErrorMessage).

## 4. CHAR/VARCHAR V90 hardening

**Not executed** — requires explicit authorization (schema migration). Still optional next step.

## Conclusion

Backend unit/engine suite is **green on JDK 21**. Playwright E2E is a **gap to implement**, not a failed run.
