# CUSTOMER LIVE GATE HANDOFF

**Master package:** PRODUCT-COSTING-SOFTWARE-MASTER-V179.zip (support refresh: **V180**)  
**Date:** 2026-08-15  
**Host:** Windows + Docker Desktop (customer only)  
**Agent offline status:** Development for Phase-1 / Phase-2 / P3-01 closed. Live PASS is **your** evidence only.

---

## 1. Scope of this package

| Area | Content |
|------|---------|
| Phase-1 | Costing, RFQ, workflow, Central Access, VS, Helping Agent (read-only) |
| Phase-2 | Deep variance, lifecycle, mixed costing, make-vs-buy, SAP CO **inbound** staging |
| Phase-3 P3-01 | Purchase Request MVP + hardening A–G |
| Flyway | **V1–V98** |

**Not in live gate scope:** live SAP MM post, AI approve/release, inventing FX, unfreezing undefined P3-05..07.

---

## 2. Offline evidence (honest)

| Suite | Agent status |
|-------|----------------|
| Backend unit | Tests written; **re-run** `mvn -DskipITs test` on your host |
| FE vitest / build | Re-run on your host |
| Playwright / Docker | **NOT RUN** on packaging agent — **your gate** |

Do **not** treat agent packaging as production PASS.

---

## 3. Bring-up sequence (Windows + Docker Desktop)

```bat
cd PRODUCT-COSTING-SOFTWARE
set JWT_SECRET=local-dev-secret-must-be-at-least-32-chars!!
docker compose up --build -d
docker compose ps
curl http://localhost:8080/actuator/health
curl -I http://localhost:8088/
```

**Networking:** Browser on `http://localhost:8088` must call `localhost:8088/api/...` (proxy), not `:8080` directly from the SPA.

```bat
set BASE=http://127.0.0.1:8080
bash scripts/prod-smoke.sh
bash scripts/helping-agent-smoke.sh
bash scripts/purchase-smoke.sh

cd backend
mvn -DskipITs test

cd ..\frontend
npm ci
npx vitest run
npm run build
npx playwright test
npx playwright test e2e/phase2-wave-b.spec.ts
```

---

## 4. Module entitlements (required before feature smoke)

| Module | Needed for |
|--------|------------|
| `PRODUCT_COSTING` | BOM, should-cost, MvB, lifecycle, mixed, SAP estimates UI |
| `VENDOR_SELECTION` | VS, TCO, qualification APPROVED |
| `PURCHASE` | Purchase Requests nav + APIs (hardening F) |

Platform Admin → Customers → entitlements. Backend is authoritative.

---

## 5. Phase-1 + Phase-2 manual smoke

1. **Should-Cost** — scenario; variance + deep variance (missing → DATA NOT AVAILABLE).  
2. **Dashboard** — program context; totals or NOT AVAILABLE.  
3. **Lifecycle** — volume curve; projection.  
4. **Mixed Costing** — legs, 100% ratios; calculate; freeze.  
5. **Make vs Buy** — MAKE + BUY; draft compare; **human** decide with reason.  
6. **SAP Cost Estimates** — POST sample per CPI checklist; map; compare (no live RFC).  
7. **Helping Agent** — passive FAB; classification; **no writes**.  
8. Cross-flow: BOM → Should-Cost → Target → RFQ → Comparison → VS → human approval.

---

## 6. Phase-3 P3-01 Purchase Request smoke

**Prerequisite:** `PURCHASE` entitlement ON; at least one supplier with qualification lifecycle **APPROVED**; preferably ACCEPTED quote on same BOM.

1. MvB → human **APPROVE_BUY** or **APPROVE_MIXED** (status DECIDED).  
2. Create PR from simulation **or** manual + `manualOverrideReason`.  
3. **A:** non-APPROVED / BLOCKED / SUSPENDED supplier → rejected.  
4. **B:** quote must match BOM node + supplier; non-ACCEPTED needs override.  
5. **C:** TCO `AVAILABLE` only if complete VS TCO exists; else `NOT_AVAILABLE`.  
6. **E:** Submit → SUBMITTED; optional workflow if definition exists; notification to submitter.  
7. Human **decide** APPROVE/REJECT + reason → `decidedBy` set.  
8. **Release** blocked if any line `dataStatus=NOT_AVAILABLE`.  
9. **Export package** — JSON only; policy notes; **does not post to SAP MM**.  
10. Toggle `PURCHASE` OFF → nav hidden; API 403.

```bat
bash scripts/purchase-smoke.sh
```

---

## 7. Pass criteria

- Health UP; FE loads; API via same-origin proxy  
- `prod-smoke.sh`, `helping-agent-smoke.sh`, `purchase-smoke.sh` PASS (or documented skip)  
- `mvn -DskipITs test` green on **your** machine  
- Playwright green (or documented exceptions)  
- No invented zeros / FX; NOT_ELIGIBLE cannot win MvB  
- Helping Agent read-only  
- No cross-tenant leakage  
- P3-01 gates A–G behavior as above  

---

## 8. Do not

- Claim production PASS without live evidence on this host  
- Expect AI auto-approve / auto-release  
- Invent FX rates or treat missing as zero  
- Expect app to post to SAP MM / ME21N  
- RELEASE PR when any line is `NOT_AVAILABLE` (unless product changes §12)  
- Implement P3-05..07 without SRS  

---

## 9. Sign-off checklist

| # | Check | PASS/FAIL | Notes |
|---|--------|-----------|-------|
| 1 | `actuator/health` UP | | |
| 2 | FE `8088` proxy to API | | |
| 3 | `prod-smoke.sh` | | |
| 4 | `helping-agent-smoke.sh` | | |
| 5 | `purchase-smoke.sh` | | |
| 6 | `mvn -DskipITs test` | | |
| 7 | Playwright (incl. phase2-wave-b) | | |
| 8 | PRODUCT_COSTING / VENDOR_SELECTION entitlements | | |
| 9 | PURCHASE on/off controls nav + API | | |
| 10 | MvB human decide | | |
| 11 | PR create from DECIDED MvB | | |
| 12 | Reject non-APPROVED supplier | | |
| 13 | Reject RELEASE on NOT_AVAILABLE | | |
| 14 | Export package policy notes (no SAP MM) | | |
| 15 | Helping Agent read-only | | |
| 16 | No cross-tenant data | | |

**Signed:** _________________ **Date:** ________ **Package:** V179 / V180 support

---

## 10. Defect protocol

Use `LIVE_GATE_DEFECT_TEMPLATE.md`.

Report: title, package version, steps, expected vs actual, browser, Network/console, `docker compose ps`, tenant/module entitlements.

Engineering: reproduce → minimal fix → regression → new **MASTER-Vxxx** zip.

---

## 11. Support documents

| Doc | Use |
|-----|-----|
| `scripts/prod-smoke.sh` | Auth + basic API |
| `scripts/helping-agent-smoke.sh` | Agent read-only / ACCESS-07 |
| `scripts/purchase-smoke.sh` | PURCHASE module gate smoke |
| `LIVE_GATE_DEFECT_TEMPLATE.md` | Defect report form |
| `SAP_CPI_INTEGRATION_CHECKLIST.md` | SAP inbound path |
| `FEATURE_P3_01_HARDENING_ABD.md` / `FEATURE_P3_01_HARDENING_CEFG.md` | Hardening detail |
| `API_SPEC.md` / `DATABASE_SCHEMA.md` | Contracts |
| `DOCKER_COMPOSE_NETWORKING_REPORT.md` | Ports / proxy |

---

*End of customer live gate handoff (V179 product + V180 support materials).*
