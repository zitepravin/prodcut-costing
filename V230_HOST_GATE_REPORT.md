# V230 — Host Gate (Docker + Playwright + Live Security)

**Package:** PRODUCT-COSTING-SOFTWARE-MASTER-V230.zip  
**Date:** 2026-08-17  
**Base:** V229 (Maven GREEN)

---

## A. Executive Summary

V230 was attempted on the packaging agent to close the remaining release gates:

| Gate | Result |
|------|--------|
| Docker compose | **BLOCKED** — `docker` not installed; package install unavailable |
| Playwright E2E | **BLOCKED** — requires running application stack |
| Live CUSTOMER_ADMIN 403 | **BLOCKED** — requires running backend |
| Maven (V229 evidence) | **PRIOR PASS** — 594 tests, 0 failures (V229) |
| FE typecheck (V229) | **PRIOR PASS** |

**Final decision: READY WITH MINOR GAPS** (unchanged from V229)

Per V229/V230 rules: **READY is not claimed** without Docker + Playwright execution evidence.

---

## B. V229 Carry-Forward

| Item | V229 | V230 |
|------|------|------|
| Maven suite | PASS (594/0 fail) | Evidence retained; not re-run (agent JDK/Maven paths ephemeral) |
| FE typecheck | PASS | Retained |
| Docker | NOT RUN | **Still blocked on agent** |
| Playwright | NOT RUN | **Still blocked on agent** |
| Governance unit tests | PASS | Retained |

---

## C. Agent Environment Evidence

```
docker: command not found
/var/run/docker.sock: No such file or directory
apt-get install docker.io: Unable to locate package
```

No alternative container runtime (podman) present.

---

## D. Host Runbook (Customer / CI Machine)

Delivered executable script:

`scripts/v230-host-gate.sh`

```bash
cd PRODUCT-COSTING-SOFTWARE
export JWT_SECRET="$(openssl rand -base64 48)"
./scripts/v230-host-gate.sh
```

Manual equivalent:

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
docker compose up --build -d
docker compose ps
# wait until backend health UP on :8080

cd frontend
npm ci && npx playwright install chromium
PLAYWRIGHT_BASE_URL=http://127.0.0.1:8088 npm run test:e2e
```

Live security (after obtaining CUSTOMER_ADMIN JWT):

```bash
curl -s -o /dev/null -w "%{http_code}\n" -X POST \
  http://127.0.0.1:8080/api/v1/master/materials/{id}/approve \
  -H "Authorization: Bearer $TOKEN_CUSTOMER_ADMIN"
# Expect 403 (or 401 if token invalid) — never 200
```

COSTING_MANAGER token on same endpoint after valid PENDING_APPROVAL id: expect 200.

---

## E. Playwright Suite Present

Under `frontend/e2e/`:

- smoke.spec.ts  
- customer-admin-security.spec.ts  
- module-entitlement.spec.ts  
- navigation-hierarchy.spec.ts  
- vendor-selection*.spec.ts  
- phase2-wave-b.spec.ts  
- helping-agent.spec.ts  

Config: `playwright.config.ts` — BASE_URL via `PLAYWRIGHT_BASE_URL` (default Vite 5173; Docker FE uses **8088**).

---

## F. Security Matrix (code-verified; live pending)

| Assertion | Code/Unit | Live HTTP |
|-----------|-----------|-----------|
| CUSTOMER_ADMIN → rate approve DENIED | PASS (V229) | Pending host |
| COSTING_MANAGER → rate approve ALLOWED | PASS (V229) | Pending host |

---

## G. Files Added

| Path | Purpose |
|------|---------|
| `scripts/v230-host-gate.sh` | One-shot Docker + Playwright + curl checks |
| `V230_HOST_GATE_REPORT.md` | This report |

No application business-logic changes (gate only).

---

## H. Remaining Gaps

1. Execute `scripts/v230-host-gate.sh` on a Docker-capable host  
2. Attach `docker compose ps`, Playwright log, and curl HTTP codes  
3. Then promote readiness to **READY** if all pass  

---

## I. Recommended Next Step

**Do not start V231 feature work** until host gate evidence is attached.

If host gate passes → document **READY** and stop feature churn.  
If host gate fails → fix root causes only; re-run gate.

---

## J. Final Release Decision

### READY WITH MINOR GAPS

**Objective evidence:** V229 Maven/FE green retained.  
**Blocking gap:** Docker and Playwright cannot run on this agent.  
**No false READY claim.**
