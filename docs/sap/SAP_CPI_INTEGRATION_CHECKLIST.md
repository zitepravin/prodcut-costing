# SAP → Product Costing Platform — CPI Integration Checklist

**Audience:** Customer SAP Basis / Integration / CO team  
**Our package:** MASTER-V166+  
**Our landing API:** `POST /api/v1/erp/sap/cost-estimates`  
**Auth:** Same JWT / API auth as other PRODUCT_COSTING APIs (tenant-scoped)

---

## 1. Goal

Push **SAP Product Cost Planning** cost estimates into our platform so users can:

1. See SAP total and cost components  
2. Map SAP components to our codes (MATERIAL, PROCESS, …)  
3. Compare SAP estimate vs our **should-cost** (no invented zeros)

We do **not** call SAP RFC from our multi-tenant runtime. **Customer CPI** calls us.

```text
SAP (BAPI / OData)
    →  SAP Integration Suite (CPI)
    →  HTTPS POST our API
    →  Staging → Map → Compare
```

---

## 2. SAP-side sources (typical)

| Source | Notes |
|--------|--------|
| `BAPI_COSTESTIMATE_GETLIST` | Find estimates by material/plant |
| `BAPI_COSTESTIMATE_GETDETAIL` | Cost component split |
| `BAPI_COSTESTIMATE_ITEMIZATION` | Optional line detail |
| Product master OData | Material, plant (context) |

CPI reads SAP, maps to our JSON, posts to our API.

---

## 3. Our API contract

### Land estimate

```http
POST /api/v1/erp/sap/cost-estimates
Content-Type: application/json
Authorization: Bearer <token>
```

```json
{
  "sourceSystem": "SAP",
  "sapMaterial": "BRACKET-100",
  "sapPlant": "1000",
  "costingVariant": "PPC1",
  "costingVersion": "1",
  "estimateDate": "2026-08-01",
  "currencyCode": "INR",
  "totalCost": "120.000000",
  "rawPayload": "{ optional original JSON }",
  "components": [
    { "sapComponentCode": "ZMAT", "sapComponentName": "Material", "amount": "70.000000" },
    { "sapComponentCode": "ZPRC", "sapComponentName": "Production", "amount": "50.000000" }
  ]
}
```

**Required:** `sapMaterial`, `sapPlant`  
**Recommended:** `totalCost`, `currencyCode`, `components[]`

### Other endpoints

| Method | Path | Use |
|--------|------|-----|
| GET | `/api/v1/erp/sap/cost-estimates` | List staged |
| GET | `/api/v1/erp/sap/cost-estimates/{id}` | Detail |
| POST | `.../map-components` | Re-apply mapping table |
| POST | `.../compare-should-cost` | Body: `{ "shouldCostBuildUpId": "..." }` |
| GET/PUT | `/api/v1/erp/sap/component-mappings` | Maintain SAP→our codes |

---

## 4. Component mapping (customer task)

| SAP cost component (example) | Our code |
|------------------------------|----------|
| Material / ZMAT | MATERIAL |
| Production / labor | PROCESS |
| Tooling | TOOLING |
| Packaging / freight | PACKAGING_FREIGHT |
| Overhead | OVERHEAD |
| SG&A | SGA |
| Margin / profit | PROFIT_MARGIN |

Mappings are stored per environment via PUT `/component-mappings`. Unmapped components stay visible but compare shows **DATA NOT AVAILABLE** for that side.

---

## 5. CPI iFlow checklist

- [ ] Cloud Connector (if on‑prem SAP) connected  
- [ ] RFC/OData credential in CPI secure store  
- [ ] iFlow: read cost estimate list/detail for agreed plants/materials  
- [ ] Map fields to JSON contract above (decimals as strings or numbers; we accept both)  
- [ ] HTTPS receiver to our base URL + path `/api/v1/erp/sap/cost-estimates`  
- [ ] Auth: service user JWT or agreed API key pattern with our platform team  
- [ ] Error handling: retry on 5xx; log 4xx body  
- [ ] Schedule: e.g. nightly or on estimate release event  
- [ ] No PII beyond material/plant/costing metadata  

---

## 6. Security & tenancy

- Each customer tenant has isolated data  
- CPI must use credentials for **that** customer only  
- Do not send cross-company cost estimates into the wrong tenant  

---

## 7. What we will not do (v1)

- Direct RFC from our cloud app into customer SAP  
- Write cost estimates back into SAP CO  
- Material Ledger / actual costing period close  

---

## 8. Acceptance test (joint)

1. CPI posts one known material/plant estimate  
2. UI **Operations → SAP Cost Estimates** shows the row  
3. Mapping saved for at least MATERIAL + PROCESS  
4. Compare against a should-cost build-up ID  
5. Variance matches spreadsheet check (SAP total − should-cost total)  
6. Missing component → **DATA NOT AVAILABLE**, not 0.00  

---

## 9. Contacts / artifacts

| Item | Location |
|------|----------|
| Design | `P2-SAP_CO_INTEGRATION_DESIGN.md` |
| Feature | `FEATURE_SAP_CO_INTEGRATION.md` |
| Package | `PRODUCT-COSTING-SOFTWARE-MASTER-V166`+ |

---

*End of checklist*
