# CUSTOMER LIVE GATE HANDOFF

**Master package:** PRODUCT-COSTING-SOFTWARE-MASTER-V155.zip  
**Date:** 2026-08-14  
**Agent offline status:** Development + offline regression complete. Live gate is **customer-only**.

## Offline evidence (agent host)

| Suite | Result |
|-------|--------|
| Backend unit (`mvn -DskipITs test`) | **PASS** — 458 run, 0 fail, 1 skip (Flyway IT needs Docker) |
| Helping Agent unit | Included in 458 |
| Live Docker / Playwright / localhost | **BLOCKED** on agent (no route to your Windows Docker) |

## What is in the package

- Full Product Costing + Vendor Selection + Billing + Helping Agent
- Docker Compose networking fix (`VITE_API_BASE_URL`, `pcs-net`)
- AI_AGENT knowledge (5100 Q&A) + RULES + behavior docs
- Smoke scripts, Playwright specs, dry-run checklist

## Your live gate sequence (Windows + Docker Desktop)

```bat
cd PRODUCT-COSTING-SOFTWARE
set JWT_SECRET=local-dev-secret-must-be-at-least-32-chars!!
docker compose up --build -d
docker compose ps
curl http://localhost:8080/actuator/health
curl -I http://localhost:8088/
```

**Networking check:** On http://localhost:8088, browser Network tab must show API calls to `localhost:8088/api/...` (not `:8080`).

```bat
set BASE=http://127.0.0.1:8080
bash scripts/prod-smoke.sh
bash scripts/helping-agent-smoke.sh
cd frontend
npm ci
npx playwright test
```

Manual:

1. `HELPING_AGENT_LIVE_ACCEPTANCE_CHECKLIST.md`
2. `PRODUCTION_DRY_RUN_CHECKLIST.md`
3. Cross-flow: BOM → Should-Cost → Target → RFQ → Comparison → Approval

## Pass criteria

- Health UP; FE loads
- Smoke scripts PASS
- Helping Agent: passive FAB, chat with classification, no writes
- No cross-tenant leakage in CAM tests you run
- Unit/Playwright green on your machine

## Do not

- Claim production PASS without live evidence
- Unfreeze Purchase/PO modules without product decision
- Use knowledge Q&A as financial source of truth

## Support docs

| Doc | Use |
|-----|-----|
| DOCKER_COMPOSE_NETWORKING_REPORT.md | Ports, proxy, VITE fix |
| HELPING_AGENT_FINAL_SPEC_COMPLIANCE.md | Chatbot vs final spec |
| HELPING_AGENT_PASSIVE_ICON_BEHAVIOR.md | Opt-in FAB |
| FULL_SCALE_QA_REPORT.md | Module matrix |
| AI_AGENT/PRODUCT_COSTING_HELP_AGENT_RULES.md | Safety rules |
