# Playwright E2E — full suite live stack

**Date:** 2026-08-14  
**Master:** PRODUCT-COSTING-SOFTWARE-MASTER-V113  
**Command:** `npx playwright test -c playwright.config.ts`

## Environment

| Component | Detail |
|-----------|--------|
| MySQL | 8.0.46 — Flyway **V90** |
| Backend | Spring Boot :8080 — login **200** |
| Frontend | Vite :5173 |
| Browser | Chromium |
| Credentials | `admin` / `ChangeMe!Now1` |

## Aggregate result

| Suite | Tests | Result |
|-------|-------|--------|
| `e2e/smoke.spec.ts` | 4 | **PASS** |
| `e2e/vendor-selection.spec.ts` | 4 | **PASS** |
| `e2e/vendor-selection-360.spec.ts` | 2 | **PASS** |
| `e2e/vendor-selection-decision.spec.ts` | 2 | **PASS** |
| **Total** | **12** | **12 passed (~1.4 min)** |

## Suites

- Smoke: login UI, root redirect, credentials, VS route  
- Vendor Selection: nav, 360 tab, Eligibility/Recommendation, deep-link  
- 360 fixture: ACME-BRG load 360, list suppliers  
- Decision boundary: lowest price never best; human approval language  
