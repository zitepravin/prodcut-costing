# Demo walkthrough — how each feature works (testing)

**Package:** MASTER V221  
**Purpose:** Click through every major function with **demo records already in the database**.

## Login

| User | Password | Role / Dashboard persona |
|------|----------|--------------------------|
| `demo.systemadmin` | `ChangeMe!Now1` | SYSTEM_ADMINISTRATOR |
| `demo.master` | `ChangeMe!Now1` | MASTER_DATA_ADMIN |
| `demo.engineer` | `ChangeMe!Now1` | COSTING_ENGINEER |
| `demo.manager` | `ChangeMe!Now1` | COSTING_MANAGER |
| `demo.finance` | `ChangeMe!Now1` | FINANCE_CONTROLLER |
| `demo.buyer` | `ChangeMe!Now1` | PURCHASING_BUYER |
| `demo.sourcing` | `ChangeMe!Now1` | SOURCING_LEAD |
| `demo.programcost` | `ChangeMe!Now1` | PROGRAM_COST_MANAGER |
| `demo.process` | `ChangeMe!Now1` | PROCESS_ENGINEER |
| `demo.tooling` | `ChangeMe!Now1` | TOOLING_ENGINEER |
| `demo.materialbuyer` | `ChangeMe!Now1` | COMMODITY_MATERIAL_BUYER |
| `demo.industrial` | `ChangeMe!Now1` | INDUSTRIAL_ENGINEER |
| `demo.productcost` | `ChangeMe!Now1` | PRODUCT_COST_ENGINEER |
| `demo.purchasemanager` | `ChangeMe!Now1` | PURCHASING_MANAGER |
| `demo.platformowner` | `ChangeMe!Now1` | PLATFORM_OWNER |
| `demo.customeradmin` | `ChangeMe!Now1` | CUSTOMER_ADMIN |
| `admin` | `ChangeMe!Now1` | Bootstrap SYSTEM_ADMINISTRATOR (V9) |

Use customer/tenant **DEFAULT** when prompted. Full matrix: `docs/v292-merge/ROLE_DEMO_USER_MATRIX.md`.

## Core demo context (V18)

| Object | ID / code | Where to see it |
|--------|-----------|-----------------|
| Program | `EV-CX-2027` | Dashboard / BOM / RFQ |
| BOM MAKE part | `b1000000-0000-4000-8000-000000000002` (control arm) | BOM, Should-Cost, Scenarios, Standard Cost, Financial, Auto-Intel |
| BOM BUY part | `b1000000-0000-4000-8000-000000000003` (bearing) | RFQ / Quotes |
| Should-cost | `e1000000-0000-4000-8000-000000000001` | Should-Cost, ECN, What-if, Standard Cost source |
| Materials | `CRS-1010`, `AL-6061` | Materials |
| Suppliers | ACME, NIPPON | Vendor Selection / RFQ |

---

## Click path by feature (SAP A–E + market)

### 1. Costing Variants (A1) — Product Costing → Costing Variants / Advanced Costing
- Open list → see **7** demo variants: `STD-PROD`, `PLAN-YR`, `QUOTE-BUY`, `SIM-LIGHT`, `EXPORT-EUR`, `PROTO`, `LEGACY`.
- **What it does:** Stores valuation rules (lot size, scrap, yield, price source) reused by costing — does not invent costs by itself.

### 2. Should-Cost / Quantity structure (A2)
- Open **Should-Cost**, load BOM node `b1000000-…0002`.
- **What it does:** Bottom-up total from material/process/tooling/packaging; baseline for scenarios and ECN.

### 3. Scenarios + Additive (B4/B5) — `/scenarios`
- BOM node ID = control arm UUID above → **Load**.
- See **6** scenarios (BASE approved/locked, SUP-A, MAKE, ECN-12, VOL-UP, FX-EUR).
- Open BASE → **6 additive lines** (prototype, launch, tooling, engineering, freight, customer-specific).
- **What it does:** Alternative cost stories without overwriting approved BASE; additives are separate one-time/special costs.

### 4. Procurement alternatives (B6) — `/procurement-alternatives`
- Enter BOM node → **Evaluate** (optionally subcontract/external unit costs).
- **What it does:** Ranks MAKE / BUY / SUBCONTRACT / EXTERNAL / MIXED from live data; recommendation is **not** auto-award.

### 5. Standard costs (C1) — `/standard-costs`
- Load BOM node → see versions **SUPERSEDED / RELEASED / DRAFT**.
- Try **Mark → Release → Freeze** on DRAFT (manager role).
- **What it does:** Lifecycle of a formal unit cost separate from should-cost draft.

### 6. Financial triangle (C2–C4) — `/financial`
- Load BOM node → **Load triangle** → PLANNED / STANDARD / SHOULD / ACTUAL / SAP buckets.
- **6 actual cost periods** (2026-01…06) already imported for the control arm.
- Valuation policies: `NA-STD`, `EU-GRP`, `IN-LOCAL`.
- **What it does:** Compares buckets; missing stays DATA_NOT_AVAILABLE; actuals are import-only.

### 7. Mfg intelligence (D1/D2) — `/mfg-intel`
- **7 activity types** (PRESS-HR, CNC-HR, LABOR-DIR, …) → **Calculate cost** (qty × rate).
- **6 capacity centers** → Select PRESS-LINE-1 → **Utilization** (loads already applied).
- **What it does:** Activity cost and capacity overload status.

### 8. Automotive intel (E3/E4) — `/auto-intel`
- BOM node = control arm → **Load ECN history** → ECN-2026-0012 / 0018 with unit + annual $.
- **Run what-if** (e.g. material +10%) or open existing simulations.
- **What it does:** ECN impact and central what-if shocks on should-cost baseline.

### 9. Benchmarks + analytics — `/benchmarks` + API analytics
- Packs/rates from V105; analytics: should vs quote vs benchmark for a BOM node.
- **What it does:** Reference rates vs internal should-cost / quotes.

### 10. Supplier portal — `/supplier-portal` (Sourcing)
- Portal login / My RFQs / submit quote (buyer still reviews).
- Core RFQ demo: `RFQ-2026-0042` with ACME accepted quote.

### 11. Classic modules (already seeded in V18)
Materials, Machines, BOM tree, Process, Tooling, Packaging, Target, Cost Comparison, RFQ, Vendor Selection, ERP Staging.

---

## How to apply seed

On a DB that already ran Flyway through **V117**:

```bash
# Full stack: Flyway runs V118 automatically on startup
docker compose up -d
# or
mvn -pl backend flyway:migrate
```

Fresh install: V18 + V118 both apply in order.

---

## Testing tip

1. Always start from **module landing** (Product Costing / Sourcing / Operations), not only the side list.  
2. Paste the **control arm BOM node ID** into forms that ask for BOM node.  
3. Prefer **demo.engineer** for costing screens and **demo.manager** for release/approve actions.
