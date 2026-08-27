# Handoff — Production Hold (MASTER V131)

## Status
Platform is **feature-frozen for new modules** (no Purchase / PO / Order Tracking) until customer-host production dry-run is signed PASS.

## Verified baseline (agent host)
| Check | Result |
|-------|--------|
| Backend unit | 432 PASS |
| Host Flyway IT | V90 PASS |
| Vitest | 17 PASS |
| Module ACCESS-03 / ACCESS-07 | PASS |
| Actuator health | 200 UP (mail health disabled) |
| Playwright | 14 PASS (prior live stack) |
| prod-smoke.sh | PASS |

## What ops must do next
1. Open `docs/PRODUCTION_DRY_RUN_CHECKLIST.md`
2. Deploy with **prod** profile, real `JWT_SECRET`, TLS, rotated admin password
3. Run `./scripts/prod-smoke.sh` against HTTPS BASE
4. Sign checklist PASS / FAIL

## What developers must not do
- Do not implement Purchase/PO/Order modules
- Do not invent UI polish without a row in `docs/UI_DEFECT_BACKLOG.md`
- Do not weaken `@RequiresModule` or tenant isolation

## Package contents (minimum)
- `frontend/` `backend/` `docs/` `AI_AGENT/` `scripts/`
- Flyway V1–V90 under `backend/src/main/resources/db/migration`
- Continuity: PROJECT_STATUS, CURRENT_CONTEXT, NEXT_AGENT, CHANGELOG, API_SPEC, DATABASE_SCHEMA

## Resume development after dry-run PASS
Only then open backlog for next approved SRS scope (still not automatic Purchase/PO).
