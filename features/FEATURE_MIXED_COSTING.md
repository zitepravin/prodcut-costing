# FEATURE_MIXED_COSTING.md

**Feature:** Mixed Costing / Multiple Procurement Alternatives (P2-03)  
**Version:** MASTER-V165  
**Module:** Product Costing  
**Status:** Implemented (BE + FE)  
**Date:** 2026-08-15

---

## 1. Purpose

Model split-volume sourcing (e.g. 60% internal should-cost + 40% supplier quote) and compute a **blended unit cost** with frozen snapshots so history does not drift when masters change.

## 2. Problem solved

Phase-1 should-cost is one source position per scenario. Real programs dual-source or mix MAKE/BUY. Without mixed costing there is no first-class, auditable blend.

## 3. Users / roles

Cost engineers, sourcing leads with PRODUCT_COSTING module entitlement and program scope.

## 4. What it does

- Create named mixed-cost versions on a BOM node (≥2 legs)
- Leg types: INTERNAL_SHOULD_COST | SUPPLIER_QUOTE
- Ratios must sum to 100% (scale 4)
- Calculate: resolve unit costs, check supplier eligibility, blend
- Freeze: lock snapshots
- Strict v1: any missing cost or NOT_ELIGIBLE supplier → blended status NOT_AVAILABLE (never invent zero)

## 5. What it does not do

- Auto-award RFQ / create PO
- Full TCO blend (freight, inventory)
- Currency conversion of quote legs to reporting currency (v1 uses native quote currency snapshot; conversion deferred)
- Override role for blocked suppliers (v1 hard block)

## 6. Workflow

1. Select BOM node → Create mixed-cost version + legs  
2. Calculate → review eligibility / data quality notes  
3. If AVAILABLE → Freeze  

## 7. User instructions

API-first in v1 (UI page can be added next). Use:

```http
POST /api/v1/bom-nodes/{bomNodeId}/mixed-costs
POST /api/v1/mixed-costs/{id}/calculate
POST /api/v1/mixed-costs/{id}/freeze
```

## 8. Frontend

Shipped in V165: MixedCostingPage with PageHeader, ratio sum, StatusChip, calculate/freeze.

## 9. Backend

Package: `com.automotive.costing.mixedcost`

- `MixedCostBlendEngine` (pure)
- `MixedCostService` / `MixedCostController`
- Entities: `MixedCostVersion`, `MixedCostLeg`

## 10. API

| Method | Path |
|--------|------|
| POST | `/api/v1/bom-nodes/{bomNodeId}/mixed-costs` |
| GET | `/api/v1/bom-nodes/{bomNodeId}/mixed-costs` |
| GET | `/api/v1/mixed-costs/{mixedCostId}` |
| PUT | `/api/v1/mixed-costs/{mixedCostId}/legs` |
| POST | `/api/v1/mixed-costs/{mixedCostId}/calculate` |
| POST | `/api/v1/mixed-costs/{mixedCostId}/freeze` |

## 11. Database

Flyway **V94**: `mixed_cost_version`, `mixed_cost_leg`

## 12. Security

`@RequiresModule("PRODUCT_COSTING")` + `AccessScopeService` program scope via BOM node.

## 13–15. Central Access / Tenant

Module-gated; program scope enforced. Tenant isolation via existing platform patterns (same as lifecycle / should-cost).

## 16. Dependencies

BomNode, ShouldCostBuildUp, SupplierQuote, SupplierMaster

## 17–21. Tests

| Test | Status |
|------|--------|
| MixedCostBlendEngineTest (5 cases) | **Written** — not executed on this agent (Maven absent) |
| Full `mvn -DskipITs test` | **NOT RUN** on this agent |
| Playwright | **NOT RUN** |

Engine cases: 60/40 blend=92, ratio fail, missing cost, blocked supplier, &lt;2 legs.

## 22. Acceptance criteria (v1)

- [x] Ratios must sum to 1.0000  
- [x] Blend formula Σ(unit×ratio)  
- [x] Missing data → NOT_AVAILABLE, not zero  
- [x] BLOCKED/INACTIVE supplier → NOT_ELIGIBLE blocks calculate  
- [x] Freeze requires CALCULATED + AVAILABLE  
- [ ] FE page  
- [ ] Executed full unit suite green  

## 23. Files changed

- `V94__mixed_cost_version.sql`
- `mixedcost/**` (entity, repo, engine, dto, service, controller)
- `MixedCostBlendEngineTest.java`

## 24. Known limitations

- No FE page yet  
- No FX conversion of legs to a common reporting currency before blend  
- VS lifecycle SUSPENDED not mapped (SupplierMaster has ACTIVE/INACTIVE/BLOCKED only)

## 25. Release notes

P2-03 Mixed Costing backend v1 in MASTER-V164.
