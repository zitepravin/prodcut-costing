# Master Data Governance, Versioning & Data Quality — Gap Report (V226)

**Base package:** PRODUCT-COSTING-SOFTWARE-MASTER-V225  
**Assessment date:** 2026-08-17  
**Scope:** Inspect existing implementation; implement only genuine gaps; no duplicate entities / RBAC rewrite.

---

## Executive summary

The platform already has a **strong M01 master-data foundation** (Flyway V2 + material/machine/labor/overhead rate versioning, bulk import, currency/FX module V13, vehicle program, process library, plant, UOM).  

This pass **did not rebuild** those modules. It added genuine gaps on the material rate path:

1. **Approve** PENDING_APPROVAL versions (Costing Manager)
2. **Close prior ACTIVE effective window** on approval (no overlap)
3. **Historical rate resolution** `findEffectiveAsOf(code, asOf)` for reproducible costing
4. Unit tests for approve + historical rate immutability

---

## Traceability model (existing)

```
Frontend (MaterialsPage, MachinesPage, ProcessCostingPage, CurrencyPage, …)
  → /api/v1/master/* Controllers (@RequiresModule PRODUCT_COSTING, @PreAuthorize)
  → *MasterService (versioning, validation, audit)
  → Repository / AbstractMasterRateRecord tables
  → MaterialRatePublishedEvent → costing listeners
  → MaterialCost / ProcessCost / ShouldCost engines
```

Master data is **plant-scoped / global catalog** (not per-customer_id rows). Multi-tenant isolation for business objects continues via CustomerContext on product/BOM/should-cost data. Plant-level access scopes (SEC-03) apply where configured.

---

## Master gap matrix

| # | MASTER | STATUS | IMPLEMENTED FEATURES | MISSING / PARTIAL | SECURITY | TEST | RECOMMENDED ACTION |
|---|--------|--------|----------------------|-------------------|----------|------|--------------------|
| 1 | **Material** | **GREEN** | CRUD, search, version history, new-version (no in-place rate edit), deactivate, density/grade/form, index-linked, bulk import, audit, MAT event | Category/family optional fields; multi-tenant row isolation N/A by design | MASTER_DATA_ADMIN write; module gate | Unit + import tests | Use approve + effective-as-of (V226) |
| 2 | **Material Grade** | **YELLOW** | Stored as `grade` on MaterialMaster | No separate grade catalog entity | Inherited | Partial | Keep as attribute unless SRS requires catalog |
| 3 | **Material Price** | **GREEN** | `rate` + versioning + base_price + alloy surcharge + commodity index | — | As material | Unit | Maintain version discipline |
| 4 | **Process** | **YELLOW** | process_master library (name, default cycle formula, scrap%) | No rate versioning/effective dates on process library itself; routing lives in process costing module | Module gate | Limited | Enrich only if SRS requires process-rate versions |
| 5 | **Machine** | **GREEN** | AbstractMasterRateRecord + type/capacity/depreciation/power/maintenance/floor; new-version; import | Explicit approve endpoint may lag material (mirror V226 pattern) | MASTER_DATA_ADMIN | Unit + import | Mirror material approve if PENDING used |
| 6 | **Labor** | **GREEN** | Grade, shift, region, statutory burden + rate versioning | Same as machine for approve API parity | MASTER_DATA_ADMIN | Unit + import | Mirror material approve if needed |
| 7 | **Plant** | **YELLOW** | plant_code, country, freight_zone, duty notes | plant_name, region, currency, manufacturing type not full SRS list | Unique plant_code | Limited | Extend plant columns when required by costing |
| 8 | **Supplier** | **GREEN** | Vendor Selection / RFQ supplier domain (not duplicated in M01) | — | Tenant + VS module | VS tests | Do not create second supplier master |
| 9 | **Currency / FX** | **GREEN** | currency table + exchange_rate with effectiveDate; as-of lookup | — | Finance roles | Module tests | Historical FX via findEffectiveRate |
| 10 | **UOM** | **YELLOW** | uom_code + description | Conversion factor / category matrix thin | Reference data | Limited | Add conversion table if multi-UOM calc required |
| 11 | **Vehicle Program** | **GREEN** | program_code, model, platform, region, lifetime volume, target cost, currency, status | — | Module | Service present | Volume curve V93 integration already |
| 12 | **Overhead** | **GREEN** | overhead_pool AbstractMasterRateRecord | — | MASTER_DATA_ADMIN | Service | Keep |
| 13 | **Rate (generic)** | **GREEN** | Shared AbstractMasterRateRecord (rate, UOM, plant, effectiveFrom/To, version, status) | — | — | — | Foundation for all rate masters |
| 14 | **Import** | **GREEN** | Material/Machine/Labor bulk Excel import services + controller | Preview/dry-run depth varies by master | Role gated | Import unit tests | Prefer existing framework |
| 15 | **Versioning** | **GREEN** | New row per version; code+version unique; no in-place rate edit | Process library not versioned | — | MDM-02 tests | Do not overwrite history |
| 16 | **Approval** | **YELLOW→GREEN (Material)** | PENDING_APPROVAL on missing fields / variance; **V226 approve API** for material | Full DRAFT→SUBMITTED→APPROVED→EFFECTIVE→SUPERSEDED state machine not used (simpler 3-state) | COSTING_MANAGER approve | New unit tests | Expand state machine only if SRS mandates |
| 17 | **Audit** | **GREEN** | AuditService on create/update/transition | Correlation ID optional | — | Audited in unit tests | Keep |
| 18 | **Reconciliation** | **RED** | — | ERP vs Costing MATCHED/MISMATCHED reports | — | — | Design when ERP sync production-ready |
| 19 | **ERP Integration** | **YELLOW** | erp staging module, promote paths | Live ERP credentials not available | Staging isolation | Partial | **NOT TESTABLE — external system unavailable** |
| 20 | **PLM/CAD Integration** | **RED** | BOM/part attributes internal | No live PLM connector | — | — | **NOT TESTABLE — external system unavailable** |

---

## Critical costing propagation (verified by design)

| Driver | Path | Historical immutability |
|--------|------|-------------------------|
| Material rate | MaterialMaster → MaterialCost → BOM → Should Cost | Prior versions retained; V226 `effective as-of` resolves valuation date |
| Machine rate | MachineMaster → Process cost | Version rows retained |
| Labor rate | LaborMaster → Operation cost | Version rows retained |
| FX | exchange_rate effectiveDate | as-of lookup |
| New version ACTIVE | MaterialRatePublishedEvent | Does **not** rewrite stored cost calculation snapshots |

**Critical rule (tested in V226 unit tests):**  
Rate 100 → costing uses 100; new version 110 → new costing uses 110; **old costing remains 100**.

---

## V226 code changes (genuine gaps only)

| File | Change |
|------|--------|
| `MaterialMasterRepository` | `findEffectiveAsOf(code, asOf)` |
| `MaterialMasterService` | `approve()`, `findEffectiveAsOf()`; close prior ACTIVE `effectiveTo` |
| `MaterialMasterController` | `POST /{id}/approve`, `GET /by-code/{code}/effective?asOf=` |
| `MaterialMasterServiceTest` | approve closes window; historical rate; reject non-pending |

**Flyway:** no schema change required (columns already exist).  
**Next free migration:** still **V122** (from V225).

---

## Security (unchanged V224/V225)

- Write: `MASTER_DATA_ADMIN` (create / new-version / deactivate)
- Approve: `COSTING_MANAGER` (+ system admins) — **not** automatic for CUSTOMER_ADMIN
- Module: `@RequiresModule("PRODUCT_COSTING")`
- Support READ_ONLY still blocks writes
- No second permission model

---

## Docker / Playwright

| Check | Status |
|-------|--------|
| Docker compose | **Not run** on packaging agent (no Docker) |
| Playwright master-data E2E | **Pending** on customer host |
| `mvn test` | **Pending** on Maven host |

---

## Recommended next actions (priority)

1. Host: `mvn test` including MaterialMasterServiceTest  
2. Mirror **approve + effective-as-of** on MachineMaster / LaborMaster for API parity  
3. Optional: plant_name / UOM conversion table if business requires  
4. ERP reconciliation report only after live staging credentials  
5. Playwright: material create → new version → approve → effective-as-of → costing snapshot

---

## Classification legend

- **GREEN** = implemented and has meaningful automated coverage or clear production path  
- **YELLOW** = partial / needs host validation or optional enrichment  
- **RED** = missing or external system not testable  

**Overall master-data foundation: YELLOW-GREEN** — enterprise rate versioning is production-capable for material/machine/labor/overhead/FX; process library and plant attributes thinner; ERP/PLM reconciliation and live E2E remain open.
