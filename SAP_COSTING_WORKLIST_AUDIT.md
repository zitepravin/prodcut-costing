# SAP Costing Worklist — Phase 0 Audit

**Audit baseline:** PRODUCT-COSTING-SOFTWARE-MASTER-**V207**  
**Worklist:** SAP_Costing_Comparison_Product_Costing_Development_Worklist.docx  
**Audit date:** 2026-08-16  
**Mode:** Audit only — **no new costing-engine implementation in this step**

---

## 1. Codebase snapshot (V207)

| Area | Observation |
|------|-------------|
| Backend controllers | ~49 (incl. ShouldCost, Process, BOM, Target, SAP, Benchmark, BusinessCase, …) |
| Flyway | Through **V106** (benchmark library, business case) |
| Frontend pages | ~30 product pages + platform |
| Backend tests | ~117 `*Test.java` files (suite size varies by CI) |
| Stack | Java 21 / Spring Boot, React+TS+MUI, MySQL, Docker Compose |

---

## 2. Worklist matrix (Phase A–E)

Status legend: **COMPLETE** | **PARTIAL** | **MISSING** | **NOT APPLICABLE** | **BLOCKED**

### Phase A — SAP-style costing engine (P0)

| ID | Feature | Status | Existing evidence | Missing capability | Recommendation |
|----|---------|--------|-------------------|--------------------|----------------|
| **A1** | Costing Variant Engine | **MISSING** | SAP staging field `costing_variant` (string on import only). No domain object controlling valuation strategy, price source, lot size, sheet, scrap policy as reusable rules. | Central `CostingVariant` config reused by all engines | **Next P0 to implement** — foundation for A2–A6 |
| **A2** | Automatic Quantity-Structure Costing | **PARTIAL** | BOM nodes; process routings (ops, cycle/setup time, machine/labor codes); material cost; should-cost **generate** pulls last material/process/tooling/packaging calcs + optional overhead pool. | Single automatic pass: BOM+routing+WC+activities+lot+scrap+yield → one estimate without pre-calculated component steps | Extend should-cost engine to pull live quantity structure; do not duplicate engine |
| **A3** | Cost Component Structure 2.0 | **PARTIAL** | V92 `cost_component_definition` (MATERIAL, PROCESS, TOOLING, PACKAGING_FREIGHT, OVERHEAD, SGA, PROFIT_MARGIN); quote component amounts; maps_to_column | Hierarchy (parent/child components), fixed/variable split, multi-view roll-up, component variance as first-class | Extend V92 model; avoid second parallel component table |
| **A4** | Multilevel Costed BOM | **PARTIAL** | BOM tree/API; program cost position; should-cost per node | Full hierarchical costed BOM with qty, unit, extended, component, source, make/buy, scrap at every level in one view | Build read model on existing BOM + cost services |
| **A5** | Cost Estimate Itemization | **PARTIAL** | Should-cost build-up line totals; waterfall API; variance vs quote | Item-level: Qty × Rate × Source × Component × Rule × Amount for every line | Itemization table linked to build-up / estimate id |
| **A6** | Overhead / Costing Sheet Engine | **PARTIAL** | Overhead pools (rate, allocation base, ≥4 categories OHD-02); should-cost applies pool rate × base | Costing **sheet** (sequence of %/fixed rules), validity, multi-step base dependency, full audit of applied lines | Costing sheet on top of pools; used by variant |

### Phase B — Advanced costing (P1)

| ID | Feature | Status | Evidence | Gap | Recommendation |
|----|---------|--------|----------|-----|----------------|
| **B1** | Costing Run / Mass Costing | **MISSING** | No run entity, selection, success/fail log, rerun | Mass estimate job | After A1–A2 |
| **B2** | Lot-size Costing | **PARTIAL** | Lot/batch fields on routing; packaging; tooling amortization by volume concepts | Systematic unit-cost curve 1…100k under one variant | Use variant lot size + A2 |
| **B3** | Variance Engine | **PARTIAL** | Target variance; should-cost vs quote; version compare; cost comparison workspace | Driver split: material price, qty, labor rate, machine, FX, scrap, OH, ECN | Unify under one variance service |
| **B4** | Scenario / Version Costing | **PARTIAL** | Should-cost scenarios; cost versions (snapshot/baseline/rollback); mixed cost; make-vs-buy | Named scenario types (Supplier A/B, plant change…) with non-overwrite of approved | Extend versioning + scenarios |
| **B5** | Additive Costing | **PARTIAL** | Tooling, packaging, launch-like costs exist as modules | Explicit additive cost types on estimate (prototype, launch premium, OTE) | Additive lines on estimate |
| **B6** | Procurement Alternatives | **PARTIAL** | Make vs Buy; mixed costing; BOM make/buy | SUBCONTRACT / EXTERNAL PROCESS as first-class alternative set on node | Extend make-vs-buy |

### Phase C — Financial integration (P2)

| ID | Feature | Status | Evidence | Gap | Recommendation |
|----|---------|--------|----------|-----|----------------|
| **C1** | Standard Cost Lifecycle | **PARTIAL** | Should-cost approval statuses; workflow approvals; lock | DRAFT→…→RELEASED→FROZEN→SUPERSEDED as standard-cost object | Separate from should-cost draft if needed |
| **C2** | Planned / Standard / Actual | **PARTIAL** | Target vs should; SAP estimate compare | Explicit ACTUAL bucket without inventing numbers | Import-only actuals |
| **C3** | SAP Cost Integration | **PARTIAL** | ERP staging; SAP cost estimate staging/compare; PO link | Production actuals import, structured standard vs actual vs should | Extend SAP bridge; **no** Material Ledger clone (`SAP_INTEGRATION_BOUNDARY.md`) |
| **C4** | Multi-currency / valuation | **PARTIAL** | Currency/FX module; cost currency conversion in should-cost | Company/group/transaction/costing currency matrix + valuation date policy | Extend FX + variant currency |

### Phase D — Manufacturing intelligence (P3)

| ID | Feature | Status | Evidence | Gap |
|----|---------|--------|----------|-----|
| **D1** | Activity-Based Costing | **PARTIAL** | Labor/machine rates; routing ops; cycle templates | Full activity types + cost-center capacity logic |
| **D2** | Capacity / Utilization | **MISSING** | — | Capacity model |

### Phase E — Automotive differentiation

| ID | Feature | Status | Evidence | Gap |
|----|---------|--------|----------|-----|
| **E1** | Cost Benchmark Library | **PARTIAL** | V105 packs/rates + UI; should-cost SG&A parametric benchmarks | Scale, region packs, internal vs benchmark vs quote analytics |
| **E2** | Supplier Cost Intelligence | **COMPLETE** (core) | RFQ, quotes, TCO, scorecard, risk, 360, recommendation, human approval | Continuous enhancement only |
| **E3** | ECN Cost Impact | **MISSING** | — | ECN → delta → program $ |
| **E4** | What-if Simulation | **PARTIAL** | Make-vs-buy, mixed, lifecycle volume, business-case NPV, regional compare | Central what-if manager (material +10%, volume −20%, FX, …) |
| — | Business case / profitability | **PARTIAL** (V207) | CM/NPV/payback | Portfolio layer later |

### Platform / UX (cross-cutting)

| Topic | Status | Notes |
|-------|--------|-------|
| Multi-tenant / RBAC / CAM | **COMPLETE** (core) | Preserve |
| Workflow / Audit / Reports | **PARTIAL–COMPLETE** | Operational reports hardened V206 |
| Module navigation hierarchy | **COMPLETE** (V205+) | Dashboard > Module > Page |
| Mandatory field UI | **PARTIAL** | Theme + RequiredFieldLegend; not every form audited |
| Helping Agent | **COMPLETE** (scope) | Read-only; do not expand to auto-approve |
| Docker / Flyway install | **COMPLETE** | MASTER self-contained for schema |

---

## 3. Classification summary

| Status | Count (worklist IDs) | Items |
|--------|----------------------|--------|
| **COMPLETE** (or core complete) | 1+ | E2 Supplier intelligence; platform security baseline |
| **PARTIAL** | Majority | A2–A6, B2–B6, C1–C4, D1, E1, E4 |
| **MISSING** | Critical P0/P1 | **A1 Costing Variant**, **B1 Costing Run**, D2 Capacity, E3 ECN |
| **NOT APPLICABLE** | — | Full SAP FI/CO / Material Ledger clone (explicit non-goal) |

---

## 4. Preserve (do not rebuild)

- Should-cost full build-up + parametric + waterfall + lock/workflow  
- BOM, process routing, material/tooling/packaging cost services  
- Target costing + variance  
- Cost comparison workspace + version snapshots  
- RFQ / Vendor Selection / TCO / risk / human approval  
- ERP staging + SAP estimate compare  
- Tenant isolation, RBAC, audit  

---

## 5. First implementation target (after this audit)

Per worklist sequence and gaps:

**1. A1 — Costing Variant Engine (P0)**  
Reusable configuration object consumed by should-cost / future quantity-structure costing — **no** hard-coded rules in pages.

Then: **A2** (automatic QS costing reusing BOM/routing) → **A3** extend components → **A4/A5** costed BOM + itemization → **A6** costing sheet.

**Do not** start B1 mass run until A1–A2 exist.

---

## 6. Phase 0 gate

| Gate | Result |
|------|--------|
| Audit of worklist vs V207 code | **COMPLETE** |
| Implementation of new P0 engine items in this step | **NOT STARTED** (per instruction) |
| Duplicate modules created | **None** |

---

## 7. Session report fields

| Field | Value |
|-------|--------|
| **CURRENT VERSION** | MASTER **V207** (audit baseline) |
| **FEATURES AUDITED** | Phase A–E worklist (~22 capability IDs) + platform |
| **ALREADY COMPLETE** | Supplier intelligence core; security/CAM baseline; nav hierarchy; Docker/Flyway |
| **PARTIAL** | Should-cost QS pull, cost components V92, overhead pools, variance, SAP staging, benchmarks, business case, scenarios/versions |
| **NEWLY IMPLEMENTED** | *(this step)* **None** — audit only |
| **CURRENTLY BLOCKED** | CAD/geometry (external); SAP actuals without customer interface |
| **P0 STATUS** | **FAIL** (A1 missing; A2–A6 partial) |
| **P1 STATUS** | **FAIL** (B1 missing; others partial) |
| **BUILD / TESTS / PLAYWRIGHT / DOCKER** | Not re-run in audit-only step — prior V207 FE build + BusinessCase tests were green |
| **REMAINING WORK** | Implement **A1 Costing Variant** next, then A2…A6 per sequence |

**End of Phase 0 audit.** Implementation must not claim SAP-level capability until engine, data model, rules, audit, and tests support it.
