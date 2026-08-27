# Import Costing / Landed Cost Module (Normal Import)

**Scope:** Industry-neutral **normal import** landed cost.  
**Out of scope:** FTA, preferential tariffs, rules of origin, certificates of origin.

## Feature entitlement
- Feature code: `IMPORT_COSTING` (module `PRODUCT_COSTING`)
- Controller: `@RequiresModule("PRODUCT_COSTING")` + `@RequiresFeature("IMPORT_COSTING")`
- Tenant: `TenantAccessGuard.requireCustomerId()`

## Calculation flow
```
Inputs (origin ≠ export allowed)
  → Invoice value
  → Customs value (default = invoice; configurable later)
  → DutyEngine (AD_VALOREM / SPECIFIC / MIXED / TIERED / EXEMPT)
  → TaxEngine (recoverable vs non-recoverable)
  → Logistics charges (freight, insurance, port, clearance…)
  → LandedCostEngine (sum included components)
  → unit = total / quantity
```

Missing duty/tax/FX inputs → `reviewStatus=MISSING_DATA` (no fabricated zeros for missing rules).

## Lifecycle
DRAFT → SUBMITTED → APPROVED → LOCKED (SUPERSEDED reserved)

## APIs
| Method | Path |
|--------|------|
| POST | `/api/v1/import-costing/calculate` |
| GET | `/api/v1/import-costing/{id}` |
| GET | `/api/v1/import-costing/{id}/components` |
| DELETE | `/api/v1/import-costing/{id}` |
| POST | `/api/v1/import-costing/{id}/submit` |
| POST | `/api/v1/import-costing/{id}/approve` |
| POST | `/api/v1/import-costing/{id}/lock` |

## Database (V154)
import_duty_rule, import_tax_rule, import_incoterm_rule, import_allocation_rule,
import_shipment, import_shipment_item, import_cost_calculation, import_cost_component, import_cost_snapshot

## Tests
DutyEngine, TaxEngine, AllocationEngine, LandedCostEngine, ImportCostingService (G1/G2/G6 + validation + lifecycle)
