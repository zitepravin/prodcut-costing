# PROPOSED REQUIREMENT — SAP CO Integration (Product Cost Planning)

**Document type:** Integration design (inbound-first)  
**Baseline:** PRODUCT-COSTING-SOFTWARE-MASTER-V165  
**Date:** 2026-08-15  
**Status:** **IMPLEMENTED (backend v1)** in MASTER-V166 — contract-first, no live SAP RFC  
**Related:** ERP Staging; Should-Cost; P2-01 Cost Components; P2-04 Make-vs-Buy; SAP Help BAPIs Product Cost Planning

---

## 1. Business purpose

Customers running SAP S/4HANA (or ECC) need to:

1. **Compare** SAP released cost estimates (standard cost / cost component split) with our bottom-up should-cost.
2. **Import** material, plant, and procurement-type context so Make-vs-Buy and mixed costing use the same part identity as ERP.
3. Optionally **export** should-cost / make-vs-buy decisions as reference data — without silently posting into CO.

This is **decision-support integration**, not a Material Ledger or CKMLCP replacement.

---

## 2. SAP surfaces (verified patterns)

### 2.1 Product Cost Planning BAPIs (read)

| BAPI | Use for us |
|------|------------|
| `BAPI_COSTESTIMATE_GETLIST` | Discover estimates by material/plant |
| `BAPI_COSTESTIMATE_GETDETAIL` | Cost component split → map to P2-01 |
| `BAPI_COSTESTIMATE_ITEMIZATION` | Line-level estimate detail |
| `BAPI_COSTESTIMATE_GETEXPLOSION` | Costed multilevel BOM |

### 2.2 Master data

- Product master OData (`API_PRODUCT_SRV` / V4) — material, plant, procurement type (E/F ↔ MAKE/BUY).
- Prefer **published** APIs (Business Accelerator Hub).

### 2.3 Architecture pattern (customer landscape)

```text
SAP S/4 or ECC
    │  RFC/BAPI (Cloud Connector) or OData
    ▼
Customer middleware (CPI / Integration Suite)
    │  HTTPS + tenant credentials
    ▼
Our ERP Staging / integration API
    │
    ▼
Should-Cost · Deep variance · Make-vs-Buy
```

Multi-tenant rule: **our runtime does not open RFC to customer SAP**. Customer (or their CPI) pushes or we pull only through **customer-owned** secure endpoints.

---

## 3. Scope

### In scope (v1 proposed)

| Direction | Capability |
|-----------|------------|
| **Inbound** | Land cost estimate header + component split in ERP staging |
| **Inbound** | Land material/plant/procurement type for BOM node linkage |
| **Map** | SAP cost components → our `cost_component_definition` codes (configurable mapping table) |
| **Compare** | SAP estimate total/components vs selected should-cost (reuse deep-variance concepts) |
| **Outbound (soft)** | Export should-cost / make-vs-buy decision package (JSON/CSV/staging) for customer import into SAP |

### Out of scope (v1)

- Writing cost estimates into CO via BAPI from our SaaS  
- Material Ledger / Actual Costing period close  
- Direct RFC from multi-tenant app servers  
- Auto-change of SAP material master or BOM  

---

## 4. Domain model (proposed)

```text
sap_cost_estimate_staging
  staging_id, tenant_id, customer_id
  sap_material, sap_plant, costing_variant, costing_version
  estimate_date, currency
  total_cost
  status                     -- LANDED | MAPPED | COMPARED | ERROR
  raw_payload (JSON)
  received_at

sap_cost_estimate_component_staging
  staging_id, component_code_sap, amount
  mapped_component_code      -- our P2-01 code when mapped

sap_component_mapping
  tenant_id, sap_component_code, our_component_code
```

Reuse existing ERP staging patterns where possible; extend rather than parallel silos.

---

## 5. API (proposed — our side)

```http
POST /api/v1/erp/sap/cost-estimates          -- land estimate (from CPI)
GET  /api/v1/erp/sap/cost-estimates
POST /api/v1/erp/sap/cost-estimates/{id}/map-components
POST /api/v1/erp/sap/cost-estimates/{id}/compare-should-cost
GET  /api/v1/erp/sap/component-mappings
PUT  /api/v1/erp/sap/component-mappings
```

Compare response: SAP total vs should-cost total, component-level variance, **DATA NOT AVAILABLE** where unmapped — never invent.

**Module:** PRODUCT_COSTING (+ existing ERP staging entitlements)  
**Security:** tenant isolation; no cross-customer SAP payloads  

---

## 6. Mapping rules

| SAP concept | Our concept |
|-------------|-------------|
| Material + Plant | Part / BOM node external keys |
| Procurement type E / F | MakeBuyFlag MAKE / BUY (hint only) |
| Cost component (OKTZ structure) | `cost_component_definition` |
| Cost estimate total | Comparison baseline (not our should-cost source of truth) |

Unmapped SAP components → shown as unmapped; variance explanation lists gaps.

---

## 7. Relationship to Phase-2 features

| Feature | Integration role |
|---------|------------------|
| P2-01 Deep variance | Component-level SAP vs should-cost |
| P2-03 Mixed Costing | Independent; SAP mixed estimate can be a future BUY input |
| P2-04 Make-vs-Buy | SAP standard cost as optional reference column (not eligibility) |
| ERP Staging | Landing zone for all SAP payloads |

---

## 8. Implementation sequence (when authorized)

1. Staging tables + component mapping CRUD  
2. Land + list + map APIs  
3. Compare-to-should-cost service (BigDecimal, no invention)  
4. FE: staging list + mapping admin + compare panel  
5. FEATURE_SAP_CO_INTEGRATION.md + tests  
6. Customer runbook: CPI → our API  

**Suggested Flyway:** after P2-04 (e.g. V96) if both authorized; else next free after V95.

---

## 9. Risks

| Risk | Mitigation |
|------|------------|
| Customer SAP version variance | Support BAPI-shaped JSON contract; CPI adapts |
| Component structure differs per customer | Per-tenant mapping table |
| Legal / API policy | Published APIs only; customer-owned connectivity |
| Scope creep into Actual Costing | Explicit out of scope |

---

## 10. Decision required

| Option | Meaning |
|--------|---------|
| **A** | Authorize v1 inbound staging + compare |
| **B** | Adjust (e.g. outbound-only, or estimate header without components) |
| **C** | Defer |

---

## 11. Traceability

- SAP Help: BAPIs in Product Cost Planning  
- Investigation: session 2026-08-15  
- Baseline: MASTER-V165  
- Does **not** unblock or replace P2-04 authorization  
