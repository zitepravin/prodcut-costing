# Handoff — Production Hold (MASTER V145)

## Status
Platform remains **feature-frozen for new modules** (no Purchase / PO / Order Tracking) until customer-host production dry-run is signed PASS.

## Verified baseline (agent host, 2026-08-14)

| Check | Result |
|-------|--------|
| Backend unit (prior) | 452 PASS (excl. integration) |
| Flyway host migrate | **V91** success |
| Actuator health | 200 UP |
| prod-smoke.sh | **PASS** |
| helping-agent-smoke.sh | **PASS** (ACCESS-07 + FACT chat) |
| Playwright (prior sessions) | helping-agent + smoke + entitlement ACCESS-03; ~16 PASS |
| Vitest | 17 PASS |
| AuthContext | login awaits refreshAccess; DEFAULT tenant preferred |

## Package
`PRODUCT-COSTING-SOFTWARE-MASTER-V145.zip` (continuity from V144 auth fix)

## What ops must do next
1. Open `docs/PRODUCTION_DRY_RUN_CHECKLIST.md` (Flyway target **V91**)
2. Deploy with **prod** profile, real `JWT_SECRET`, TLS, rotated admin password
3. Run `./scripts/prod-smoke.sh` and `./scripts/helping-agent-smoke.sh` against HTTPS BASE
4. Sign checklist PASS / FAIL

## What developers must not do
- Do not implement Purchase/PO/Order modules until dry-run PASS
- Do not invent UI polish without `docs/UI_DEFECT_BACKLOG.md` row
- Do not weaken `@RequiresModule` or tenant isolation
- Do not enable `helping-agent.llm` without credentials + prompt policy

## Resume development after dry-run PASS
Only then open backlog for next approved SRS scope (still not automatic Purchase/PO).
