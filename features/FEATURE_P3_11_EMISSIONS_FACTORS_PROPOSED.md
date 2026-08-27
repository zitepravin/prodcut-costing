# FEATURE P3-11 — Optional Emissions / CO₂ Factors for Should-Cost Reporting (PROPOSED REQUIREMENT)

**Document type:** PROPOSED REQUIREMENT  
**Gap ID:** P3-11  
**Date:** 2026-08-15  
**Status:** **P3-11a IMPLEMENTED** in MASTER-V189 (material only); 11b–11d not started  
**Authorization:** `AUTHORIZE P3-xx DESIGN ONLY`  
**Scope:** Optional emissions/CO₂ factor on material/process for should-cost reporting  
**Baseline package:** MASTER-V187 (Flyway V103)

---

## 1. Business purpose

Cost engineering and sustainability teams increasingly ask:

> Beyond **what should this part cost**, what is the **indicative product carbon footprint (PCF)** associated with the same material and process assumptions used in should-cost?

This feature adds **optional, factor-based CO₂e reporting** tied to existing **material** and **process** cost drivers — **without** replacing financial should-cost, inventing missing factors as zero, or claiming certified LCA without evidence.

```text
Material qty × material emission factor
+ Process (e.g. machine hours / energy) × process emission factor
(+ optional other drivers later)
= Indicative CO₂e for the should-cost scenario
```

Financial should-cost remains the **primary** commercial output. Emissions are a **parallel reporting layer**.

---

## 2. Explicit non-goals (frozen for v1)

| Non-goal | Reason |
|----------|--------|
| Certified LCA / ISO 14067 product declaration | Requires methodology governance beyond v1 factors |
| Supplier-specific verified EPDs as automatic truth | Evidence/verification module not in scope of v1 factors |
| Converting CO₂e into cost (carbon price) automatically | Separate commercial decision; easy to misuse |
| Inventing factors when data missing | Same integrity rule as FX/cost: **DATA NOT AVAILABLE** |
| Regulatory filing / CBAM declaration engine | Out of platform scope |
| AI invents emission factors from part name | Forbidden |
| Changing should-cost money totals via emissions | Layers stay separate |

---

## 3. Design principles

1. **Optional** — tenant/module can ignore emissions entirely.  
2. **Factor-based** — explicit factor master + quantity from existing cost model.  
3. **Traceable** — factor id, version, unit, source, effective dates on every result.  
4. **Missing ≠ zero** — if factor or qty missing → `emissionsStatus = NOT_AVAILABLE`.  
5. **Versioned** — changing a factor does not rewrite locked should-cost scenarios (snapshot factors used at generate time).  
6. **Human-maintained factors** — admin/costing manager; not AI publish.

---

## 4. Proposed domain model

### 4.1 Emission factor master

| Field | Notes |
|-------|--------|
| factor_id | UUID |
| factor_code | e.g. `MAT-STEEL-CR-EU` |
| name | Display |
| category | `MATERIAL` / `PROCESS` / `ENERGY` (v1: MATERIAL + PROCESS) |
| co2e_value | BigDecimal — kg CO₂e per unit |
| unit | e.g. `KG_CO2E_PER_KG`, `KG_CO2E_PER_KWH`, `KG_CO2E_PER_HOUR` |
| reference_unit_of_measure | Aligns with material UoM or process time/energy |
| source | `MANUAL` / `LIBRARY` / `SUPPLIER_CLAIM` (unverified unless evidence later) |
| source_reference | Standard, dataset name, URL text |
| geography | Optional region code |
| version_no | Integer |
| status | `DRAFT` / `ACTIVE` / `RETIRED` |
| effective_from / effective_to | Dates |
| active_flag | Boolean |

### 4.2 Links to cost masters

| Link | Purpose |
|------|---------|
| Material master → optional `default_emission_factor_id` | Default when BOM uses material |
| Process / machine → optional `default_emission_factor_id` | Default for process step |
| Override on BOM line / process step | Optional explicit factor |

### 4.3 Should-cost emissions snapshot (per build-up)

| Field | Notes |
|-------|--------|
| build_up_id | FK should-cost build-up |
| total_co2e_kg | Nullable |
| emissions_status | `AVAILABLE` / `PARTIAL` / `NOT_AVAILABLE` |
| material_co2e_kg | Nullable |
| process_co2e_kg | Nullable |
| calculation_notes | JSON or text of factor ids + versions used |
| calculated_at | Instant |

**PARTIAL** = some lines available, some not — total may still be null or marked partial (open §11).

---

## 5. Calculation model (v1)

### Material line

```text
IF material quantity (kg or convertible) AND factor (kg CO₂e per kg) present
  THEN line_co2e = quantity_kg × factor
ELSE
  line_co2e = NOT_AVAILABLE
```

### Process line

```text
IF process driver qty (e.g. kWh or machine hours) AND matching factor unit present
  THEN line_co2e = driver_qty × factor
ELSE
  line_co2e = NOT_AVAILABLE
```

### Roll-up

```text
IF any required line NOT_AVAILABLE AND policy = STRICT
  THEN total emissions_status = NOT_AVAILABLE
ELSE IF mix of available and missing
  THEN PARTIAL (report available sum only with warning)  — or STRICT; see §11
ELSE
  total = sum(lines), status = AVAILABLE
```

**No silent zero.** Display must show **DATA NOT AVAILABLE** for missing factors.

Money should-cost formulas **unchanged**.

---

## 6. Proposed API (design only)

| Method | Path | Purpose |
|--------|------|---------|
| CRUD | `/api/v1/emissions/factors` | Factor master |
| POST | `/api/v1/emissions/factors/{id}/publish` | DRAFT → ACTIVE |
| PUT | material/process default factor links | Master data |
| GET | `/api/v1/should-cost/build-ups/{id}/emissions` | Snapshot / report |
| POST | generate should-cost (existing) | Optionally compute emissions snapshot when module on |

Module entitlement (proposed): `PRODUCT_COSTING` sufficient **or** sub-flag / `EMISSIONS` module — open §11.

---

## 7. UI (proposed)

| Surface | Behavior |
|---------|----------|
| Factor admin | Under Configuration or master data (PLATFORM / costing admin) |
| Material / process forms | Optional factor picker |
| Should-cost result | Tab or panel: **Indicative CO₂e** with status badge |
| Missing factor | **DATA NOT AVAILABLE** + which line lacks factor |
| Lock scenario | Freeze emissions snapshot with cost lock |

Helping AI may **explain** factors in use; may **not** invent factors.

---

## 8. Integration with existing modules

| Module | Interaction |
|--------|-------------|
| Should-cost | Parallel snapshot on generate/lock |
| Deep variance | No change to money variance; optional future “emissions variance” out of v1 |
| SAP CO compare | **Out of v1** — SAP components are cost, not CO₂ |
| Supplier scorecard | **Out of v1** — do not auto-score suppliers on claimed emissions |
| Configuration Center | Optional later: default library pack |

---

## 9. Phased implementation (if authorized later)

| Phase | Scope |
|-------|--------|
| **P3-11a** | Factor master + MATERIAL factors + material line CO₂e on should-cost |
| **P3-11b** | PROCESS factors + process drivers |
| **P3-11c** | Roll-up status PARTIAL/STRICT + UI panel |
| **P3-11d** | Export CO₂e on should-cost report / PDF |
| **Later** | Supplier EPD evidence, carbon price scenario (separate SRS) |

---

## 10. Testing (when implementation authorized)

- Missing factor → NOT_AVAILABLE, not 0  
- Factor version change does not alter locked snapshot  
- Unit mismatch (kg factor vs piece qty without conversion) → NOT_AVAILABLE  
- Tenant isolation on factors  
- Non-admin cannot publish factors  
- Should-cost money totals identical with emissions on/off  

---

## 11. Open decisions (product)

| # | Decision | Options / recommendation |
|---|----------|---------------------------|
| 1 | Module: fold into PRODUCT_COSTING or new `EMISSIONS`? | Recommend flag under PRODUCT_COSTING first |
| 2 | Roll-up when some lines missing: STRICT total N/A vs PARTIAL sum? | Recommend **STRICT** for total; show line-level detail |
| 3 | Unit conversion library in v1? | Recommend **No** — require matching units |
| 4 | Carbon price (€/t) scenario? | **Defer** |
| 5 | Supplier claim factors without evidence label? | Allow with source=`SUPPLIER_CLAIM` + visible label |
| 6 | Gap id | **P3-11** |

---

## 12. Approval gate

```text
DESIGN ONLY (this document)
        ↓
Product answers §11 + APPROVE FEATURE_P3_11...
        ↓
AUTHORIZE P3-11 IMPLEMENTATION
Scope: e.g. P3-11a only
        ↓
MASTER-Vxxx
```

**No runtime code under DESIGN ONLY.**

---

## 13. Traceability

| Existing asset | Role |
|----------------|------|
| Material / process cost calculations | Quantity drivers |
| ShouldCostBuildUp | Snapshot anchor |
| BigDecimal financial rules | Same precision discipline for co2e_value |
| DATA NOT AVAILABLE pattern | Missing factors |

---

*End of PROPOSED REQUIREMENT — P3-11 Optional emissions/CO₂ factors.*
