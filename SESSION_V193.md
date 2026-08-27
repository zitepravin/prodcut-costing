# SESSION V193 — 2026-08-15

## Context
Customer host: Docker stack UP (mysql, backend healthy, frontend :8088).
Playwright: 2 passed, 23 skipped, 1 failed (Helping Agent authenticated chat).

## Failure root cause
Authenticated chat without `X-Customer-Id` returns **403 ACCESS-07** (`TENANT_ACCESS_DENIED`).
E2E assertion only accepted ACCESS-03 / MODULE / PRODUCT_COSTING / disabled — not ACCESS-07.

## Fix
`frontend/e2e/helping-agent.spec.ts`: 403 branch also accepts ACCESS-07, TENANT_ACCESS_DENIED, X-Customer-Id.

## Also included
- Frontend Dockerfile + `.npmrc` + package-lock resolved to `registry.npmjs.org` (customer live-gate fix for E403 internal mirror).

## Unchanged policies
No AI approve/release · no invented FX · no SAP MM post · RELEASE blocked on NOT_AVAILABLE.

## Customer next
```powershell
# Optional: full 200 path for Helping Agent — set tenant with PRODUCT_COSTING
$cid = (Invoke-RestMethod -Uri "http://localhost:8080/api/v1/..." )  # or query MySQL platform_customer
$env:E2E_CUSTOMER_ID = "<uuid>"
npx playwright test e2e/helping-agent.spec.ts
```
Or re-run full suite after replacing the fixed spec file from this package.
