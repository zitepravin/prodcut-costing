# Phase-2 Completion Status

**Date:** 2026-08-15  
**Latest package:** PRODUCT-COSTING-SOFTWARE-MASTER-V169.zip  
**Baseline Phase-1 close:** V155  

---

## Phase-2 value-add features

| ID | Feature | Package | BE | FE | Notes |
|----|---------|---------|----|----|-------|
| P2-01 | Cost component structure + deep variance | V156+ | Yes | Yes | Flyway V92 |
| — | Program Cost Position API | V159–V161 | Yes | Dashboard | One build-up per node |
| P2-02 | Lifecycle / volume-curve forecasting | V162–V163 | Yes | Yes | Flyway V93 |
| P2-03 | Mixed costing | V164–V165 | Yes | Yes | Flyway V94 |
| P2-04 | Make-vs-Buy simulation | V168 | Yes | Yes | Flyway V96; market-aligned policy |
| — | SAP CO cost estimate integration | V166–V167 | Yes | Yes | Flyway V95; CPI inbound |

---

## Supporting work (same arc)

| Item | Status |
|------|--------|
| Historical FEATURE_*.md set | Complete |
| UX design system (PageHeader, StatusChip, …) | In tree |
| Management dashboard honesty (no FE money invention) | Done |
| SAP CPI checklist | `SAP_CPI_INTEGRATION_CHECKLIST.md` |
| API_SPEC Phase-2 sections | Updated V169 |

---

## Test honesty

| Suite | Last known |
|-------|------------|
| Backend unit (pre–P2-04 full) | V163: **472 PASS** |
| New engines (Mixed / SAP / MakeVsBuy) | Tests **written**; full suite **not re-run** on packaging agent (no Maven) |
| FE unit | **17 PASS** (vitest) |
| FE typecheck | **PASS** (V167/V168) |
| Playwright / live Docker | **NOT RUN** — customer live gate |

---

## Explicit non-claims

- No live SAP RFC from multi-tenant runtime  
- No automatic supplier approval or BOM flag change without human decide  
- No Material Ledger / actual costing clone  
- No production go-live without customer gate  

---

## Recommended customer sequence

1. Deploy **MASTER-V169** (or V168+) on Windows + Docker Desktop  
2. Run backend `mvn -DskipITs test` and FE vitest/build  
3. Smoke: Should-Cost deep variance, Mixed Costing, Make-vs-Buy, SAP staging (manual JSON post)  
4. CPI iFlow using `SAP_CPI_INTEGRATION_CHECKLIST.md` when SAP is in scope  
5. Sign live gate checklist  

---

## Next engineering options (not started)

- Source pickers (build-up / quote dropdowns) on Mixed Costing & Make-vs-Buy  
- Wire VS TCO into Make-vs-Buy BUY legs when available  
- Playwright coverage for new routes  
- Purchase / PO productization remains **feature-frozen** unless product re-authorizes  

---

*Phase-2 value-add board (P2-01..P2-04 + SAP inbound) is development-complete pending customer regression and live gate.*
