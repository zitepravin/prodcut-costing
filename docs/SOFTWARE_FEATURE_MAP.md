# Product Costing Software — Full Feature Map vs SAP CO / Market

**Package:** PRODUCT-COSTING-SOFTWARE-MASTER-V170  
**Date:** 2026-08-15  
**Purpose:** Single view of **what our software has**, how it maps to **SAP CO**, and what market tools typically add.

---

## 1. Product positioning

| System | Role |
|--------|------|
| **SAP CO-PC** | ERP product cost planning, standard cost, actual costing / material ledger, inventory valuation |
| **Our software** | Independent **should-cost + supplier intelligence** platform; can **receive** SAP cost estimates and **compare** |
| **aPriori / Teamcenter PCM-style tools** | Geometry-driven or PLM-linked should-cost |

We are **not** a full SAP CO replacement. We are complementary decision software.

---

## 2. Complete feature inventory (our software)

### A. Product structure & master data

| Feature | In our software | SAP CO equivalent |
|---------|-----------------|-------------------|
| Vehicle program | Yes | Material / plant / project context |
| BOM / BOM nodes / revisions | Yes | BOM + quantity structure |
| Make/Buy flag on node | Yes | Procurement type E/F |
| Materials master | Yes | Material master |
| Machines / rates | Yes | Work center / activity price |
| Currency & FX | Yes | Parallel currencies / exchange rates |
| ERP staging (CSV/Excel ingest) | Yes | IDoc / middleware land |
| Version control | Yes | Costing version / change |

### B. Costing engine (should-cost bottom-up)

| Feature | In our software | SAP CO equivalent |
|---------|-----------------|-------------------|
| Material costing | Yes | Material cost estimate items |
| Process / operation costing | Yes | Routing / activity |
| Tooling amortization | Yes | Special procurement / additive |
| Packaging & logistics | Yes | Overhead / condition |
| Overhead / SG&A / margin | Yes | Costing sheet / costing variant |
| Should-cost build-up | Yes | Cost estimate (CK11N style) |
| Cost component structure (P2-01) | Yes | Cost component structure (OKTZ) |
| Deep variance (quote vs should-cost) | Yes | Estimate vs actual / variance analysis (different focus) |
| Target costing | Yes | Target cost / product costing control |
| Cost comparison (multi-quote) | Yes | Quotation comparison (MM/SD side) |

### C. Phase-2 value-add

| Feature | In our software | SAP CO / market |
|---------|-----------------|-----------------|
| Program cost position (roll-up) | Yes | Costed multilevel BOM / CK13N |
| Lifecycle / volume curve | Yes | Not standard CO; planning/S&OP tools |
| Mixed costing (ratio blend) | Yes | Mixed cost estimate / procurement alternatives |
| Make-vs-Buy simulation | Yes | Make/buy decision support (not one CO screen) |
| SAP cost estimate land + compare | Yes | **Native** in SAP; we **import & compare** |

### D. Supplier intelligence (beyond pure CO)

| Feature | In our software | SAP |
|---------|-----------------|-----|
| RFQ & supplier quotes | Yes | RFQ / info records |
| Vendor Selection (scorecard, TCO, risk) | Yes | Limited; often Ariba / external |
| Supplier 360 | Yes | BP / vendor master + history |
| Eligibility gates | Yes | Source list / blocked vendor |
| AI recommendation (draft, human approval) | Yes | Not standard CO |
| Human approval workflow | Yes | Release strategy / workflow |

### E. Platform

| Feature | In our software |
|---------|-----------------|
| Multi-tenant | Yes |
| Central Access (module entitlement) | Yes |
| JWT auth + MFA capability | Yes |
| Workflow / approvals / notifications | Yes |
| Audit log | Yes |
| Reports | Yes |
| Helping AI Agent (read-only) | Yes |
| Billing / subscription gate | Yes |
| Management dashboard (counts + program position) | Yes |

### F. Explicitly NOT in our software (honest)

| Capability | Status | Typical owner |
|------------|--------|---------------|
| SAP Material Ledger / actual costing period close | **No** | SAP CO-PC-ACT |
| Inventory valuation postings to FI | **No** | SAP FI/ML |
| Production order actuals (CO-PC-OBJ) | **No** | SAP |
| Direct RFC call into customer SAP | **No** (CPI only) | Middleware |
| Purchase Orders / PO tracking | **Frozen** | Future / ERP |
| CAD geometry auto-should-cost | **No** | aPriori-class tools |
| Automatic supplier award | **No** (policy) | Human |

---

## 3. SAP CO integration — what we built

```text
SAP Product Cost Planning
   BAPI_COSTESTIMATE_* / OData
        │
        ▼
Customer CPI (required)
        │
        ▼
POST /api/v1/erp/sap/cost-estimates
        │
        ├─ Map SAP components → our MATERIAL / PROCESS / …
        └─ Compare vs should-cost (deep variance style)
```

| Our capability | Meaning |
|----------------|---------|
| Land estimate | Store SAP total + components |
| Component mapping | Per-environment SAP code → our code |
| Compare | SAP vs should-cost; **DATA NOT AVAILABLE** if missing |
| UI | `/sap-cost-estimates` |
| Live RFC from our cloud | **Not implemented** (by design) |

---

## 4. Side-by-side: “all software” view

| Business question | SAP CO | Our software | Best practice |
|-------------------|--------|--------------|---------------|
| What is the **standard cost** in ERP? | **Primary** | Import & display | Keep in SAP |
| What **should** the part cost (engineering)? | Limited | **Primary** | Our should-cost |
| Quote vs should-cost variance | Partial | **Primary** (deep variance) | Our software |
| Mixed make/buy ratios | Mixed cost estimate | **Mixed Costing P2-03** | Either; we decision-focused |
| Make vs Buy decision | Scattered | **Make-vs-Buy P2-04** | Our software + human |
| Supplier quality / TCO / risk | Weak in CO | **Vendor Selection** | Our software |
| Program-level cost position | Reports / CK | **Program Cost Position** | Both |
| Lifecycle volume impact | Planning tools | **Lifecycle P2-02** | Our software |
| Actual inventory value | **ML / Actual Costing** | Out of scope | SAP |

---

## 5. Feature count (documentation set)

Historical + Phase-2 feature docs under `features/` include (non-exhaustive):

- Authentication, MFA, Central Access, Audit  
- Dashboard, Management Dashboard, Reports  
- Materials, Machines, BOM, Master Data  
- Material / Process / Tooling / Packaging costing  
- Should-Cost, Target Costing, Cost Comparison, Currency/FX  
- RFQ, Quotes, Supplier 360, Vendor Selection  
- Workflow/Approvals, Notifications, Version Control  
- ERP Staging, Import/Export  
- P2-01 Cost Components, Program Cost Position  
- Lifecycle Volume Curve, Mixed Costing, Make vs Buy  
- SAP CO Integration  
- Helping AI Agent, Payment/Subscription  

---

## 6. Recommended use of the two systems together

1. **Engineer** builds should-cost in **our software**.  
2. **Buyer** runs RFQ + Vendor Selection + Make-vs-Buy in **our software**.  
3. **Controller** maintains standard cost and actuals in **SAP CO**.  
4. **CPI** posts SAP estimates into our staging for **compare**.  
5. Human decides; optional apply Make/Buy flag; **PO stays in ERP**.

---

## 7. Summary in one sentence

**Our software = should-cost + supplier decision intelligence + SAP estimate compare; SAP CO = ERP standard/actual product costing and financial valuation.**

---

*Source of truth for implementation: MASTER-V170 source tree + FEATURE_*.md + API_SPEC.md.*
