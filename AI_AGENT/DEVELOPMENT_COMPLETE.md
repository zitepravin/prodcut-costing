# DEVELOPMENT COMPLETE — Testing phase gate

**Date:** 2026-08-14  
**Package:** PRODUCT-COSTING-SOFTWARE-MASTER-V147.zip  
**Status:** Phase-1 Product Costing + Vendor Selection + Helping Agent **development closed** for testing.

## In scope (complete)

| Area | Status |
|------|--------|
| BOM / materials / process / tooling / packaging | Complete |
| Should-cost / target / variance / FX | Complete |
| RFQ / quotes / cost comparison | Complete |
| Workflow / approvals / notifications / audit | Complete |
| Central Access / RBAC / multi-tenant | Complete |
| Vendor Selection (360, TCO, scorecard, recommendation, human approval) | Complete |
| Billing / subscription entitlements (webhook-gated) | Complete |
| Customer Helping AI Agent (read-only) | Complete |
| UI-01 BOM auto-load, UI-02 VS suppliers, UI-05 lastBomNodeId | Fixed |
| AUTH-01 login → refreshAccess + DEFAULT tenant | Fixed |
| UI-04 dashboard unread notifications (user-scoped) | Fixed V147 |

## Explicitly out of scope (feature freeze)

- Purchase / Purchase Orders / Order Tracking module productization
- Automatic supplier approval by AI
- New costing formulas not in approved engines

## Automated regression at gate

- Backend unit: **453 PASS**, 0 fail, 1 skip (Flyway IT needs Docker)
- Vitest: **17 PASS**
- Frontend production build: **PASS**

## Testing phase (next)

Customer Windows stack:

1. Deploy V147 / `docker compose up --build -d`
2. `scripts/prod-smoke.sh` and `scripts/helping-agent-smoke.sh`
3. Playwright: `cd frontend && npx playwright test`
4. Manual business flows per FULL_SCALE_QA_REPORT / PRODUCTION_DRY_RUN_CHECKLIST
5. Record PASS/FAIL only from live evidence — never invent PASS

No further feature development until testing sign-off, except defect fixes found in testing.
