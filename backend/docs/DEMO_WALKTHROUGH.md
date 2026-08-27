# Demo walkthrough — understand every function with real-world data

**Seed:** Flyway `V18__demo_seed_data.sql`  
**Story:** 2027 compact EV crossover program **EV-CX-2027** (platform GEP3), built around a front suspension assembly.

---

## Demo accounts (password for all: `ChangeMe!Now1`)

| Username | Role | Use for |
|----------|------|---------|
| `admin` | SYSTEM_ADMINISTRATOR | Full access, user admin |
| `demo.engineer` | COSTING_ENGINEER | BOM, should-cost, process, packaging, target create |
| `demo.manager` | COSTING_MANAGER | Approvals, target activate, variance review |
| `demo.buyer` | PURCHASING_BUYER | RFQ, quotes, suppliers |
| `demo.master` | MASTER_DATA_ADMIN | Materials, machines, labor, bulk import, ERP staging |

---

## Story map (what each screen is for)

### 1. Login → Dashboard
- **Function:** Operational KPIs (programs, materials, BOM nodes, should-costs, packaging, targets, pending approvals, over-threshold variances, notifications).
- **Demo data:** Program EV-CX-2027, materials, BOM, variance over threshold, manager notification.

### 2. Materials / Machines / Import-Export
- **Function:** Master rates for steel/aluminum, presses, operators; CSV import/export.
- **Demo data:**
  - **CRS-1010** — cold rolled steel @$0.92/kg (control arm)
  - **AL-6061** — aluminum extrusion @$3.45/kg (compare alternate)
  - **PRESS-800T** — 800-ton transfer press @$85.50/hr
  - **OP-STAMP** — Grade B operator @$28.75/hr + 35% burden
  - **OH-MFG-DET** — plant overhead pool

### 3. Currency
- **Function:** Publish FX rates; convert DIRECT / INVERSE / VIA_USD.
- **Demo data:** V13 seeds USD, EUR, INR, JPY… and sample rates. Program target currency = **USD**.

### 4. BOM
- **Function:** Product structure; MAKE vs BUY; roll-up cost.
- **Demo tree (program EV-CX-2027):**

```
ASM-FR-SUSP (root assembly)          bom_node_id b100…0001
├── CA-LH-FR   MAKE  control arm     b100…0002   material CRS-1010, scrap 5%
└── BRG-HUB-FR BUY   hub bearing     b100…0003   qty 2 — cost from RFQ quote
```

- **Try:** Load program / tree; roll-up root. BUY leaf uses **accepted ACME quote $4.25** (not internal make-up).

### 5. Process / Tooling
- **Function:** Routings & cycle times; tooling amortization into piece price.
- **Demo data:** Machine/labor masters ready; create routing on `CA-LH-FR` in UI to see process cost feed should-cost / roll-up.

### 6. Packaging
- **Function:** Pack + freight + duty per piece for a BOM node.
- **Demo data:** Profile on **CA-LH-FR** — expendable carton, road/outbound freight DET-01 → NA-EAST.

### 7. Should-Cost
- **Function:** Independent nine-element cost build-up (material, process, tooling, packaging, OH, SG&A, margin).
- **Demo data:** Scenario **“Baseline DET stamping”** on CA-LH-FR — **total $7.31**.
- **Try:** Variance vs quote; submit for approval (manager inbox).

### 8. Target Costing
- **Function:** Part-level target vs should-cost or BOM roll-up; threshold flags.
- **Demo data:** Active target **$6.95** on CA-LH-FR; variance **+$0.36 (~5.2%)** → **exceeds 5% threshold**.

### 9. RFQ / Suppliers
- **Function:** RFQ event, supplier quotes, award; feeds **BUY** leaf cost.
- **Demo data:**
  - Suppliers: **ACME-BRG** (US), **NIPPON-BRG** (JP)
  - RFQ **RFQ-2026-0042** AWARDED for hub bearing
  - ACME quote **$4.25 ACCEPTED**; Nippon **$4.10 REJECTED** (lead time)

### 10. Cost Comparison
- **Function:** One BOM node — should-cost scenarios, quotes, tooling, targets side by side.
- **Try:** BOM node id `b1000000-0000-4000-8000-000000000002` (control arm) or `…0003` (bearing).

### 11. Approvals / Notifications
- **Function:** Workflow inbox; in-app alerts.
- **Demo data:** Manager has a **WARN** notification on target variance. Submit a should-cost to exercise approve/reject.

### 12. ERP Staging (`/api/v1/erp/staging`)
- **Function:** Inbound stubs for parts/BOM/prices from ERP/PLM (no auto-promote).
- **Try (API):** POST a small PART batch as `demo.master`, then validate.

---

## Quick API smoke (after login)

| Goal | Call |
|------|------|
| Program | Use program id `a2000000-0000-4000-8000-000000000001` |
| BOM tree | `GET /api/v1/bom/{programId}/tree` |
| Should-cost | `GET /api/v1/should-cost/by-bom-node/b1000000-0000-4000-8000-000000000002/scenarios` |
| Accepted quote | `GET /api/v1/rfq/quotes/accepted?bomNodeId=b1000000-0000-4000-8000-000000000003` |
| Cost comparison | `GET /api/v1/cost-comparison/by-bom-node/b1000000-0000-4000-8000-000000000002` |
| Dashboard | `GET /api/v1/dashboard/kpis` |

---

## How to load the seed

```bash
# With Docker Compose (MySQL + app)
docker compose up --build -d
# Flyway runs V1–V18 automatically on boot

# Or apply only demo SQL against an already-migrated DB:
mysql -u costing_app -p product_costing < src/main/resources/db/migration/V18__demo_seed_data.sql
```

---

## Fixed IDs cheat-sheet

| Entity | ID |
|--------|-----|
| Plant DET-01 | `a1000000-0000-4000-8000-000000000001` |
| Program EV-CX-2027 | `a2000000-0000-4000-8000-000000000001` |
| Material CRS-1010 | `a3000000-0000-4000-8000-000000000001` |
| BOM root / arm / bearing | `b100…0001` / `…0002` / `…0003` |
| RFQ / accepted quote | `d100…0001` / `…0003` |
| Should-cost / target | `e100…0001` / `…0002` |

---

**Auth remains first-party JWT only.** Rotate demo passwords before any shared/production use.
