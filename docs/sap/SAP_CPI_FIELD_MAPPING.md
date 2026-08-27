# SAP CPI Field Mapping Sheet — Cost Estimate Inbound

**Audience:** Customer SAP Integration / CPI developers  
**Our API:** `POST /api/v1/erp/sap/cost-estimates`  
**Package:** MASTER-V172 (API since V166)  
**Status:** Contract for CPI iFlow mapping (no live RFC from our SaaS)

---

## 1. End-to-end

```text
SAP ECC / S/4
  BAPI_COSTESTIMATE_GETLIST  → select estimate
  BAPI_COSTESTIMATE_GETDETAIL → cost component split
        │
        ▼
SAP Integration Suite (CPI)
  Map fields → JSON below
  HTTPS + Bearer token
        │
        ▼
POST /api/v1/erp/sap/cost-estimates
```

Optional later: `BAPI_COSTESTIMATE_ITEMIZATION` for line detail (not required for v1 compare).

---

## 2. Header field map

| Our JSON property | Type | Required | Typical SAP source | Notes |
|-------------------|------|----------|--------------------|-------|
| `sourceSystem` | string | No (default `SAP`) | Constant `"SAP"` | Or logical system name |
| `sapMaterial` | string | **Yes** | Cost estimate material / MATNR | Trim; max 40 |
| `sapPlant` | string | **Yes** | Plant / WERKS | Max 10 |
| `costingVariant` | string | No | Costing variant (e.g. PPC1) | From estimate key |
| `costingVersion` | string | No | Costing version | From estimate key |
| `estimateDate` | date `YYYY-MM-DD` | No | Costing date / KEY_DATE | ISO date only |
| `currencyCode` | string(3) | Recommended | Estimate currency | e.g. INR, USD |
| `totalCost` | decimal string/number | Recommended | Total of cost estimate | Scale up to 6; no thousands separators |
| `rawPayload` | string | No | Entire BAPI/JSON dump | Audit / support |
| `components` | array | Recommended | GETDETAIL component split | See §3 |

### Example header values

| SAP concept | Example value |
|-------------|---------------|
| Material | `BRACKET-100` |
| Plant | `1000` |
| Costing variant | `PPC1` |
| Currency | `INR` |
| Total | `120.000000` |

---

## 3. Component array field map

Each element of `components[]`:

| Our JSON property | Type | Required | Typical SAP source | Notes |
|-------------------|------|----------|--------------------|-------|
| `sapComponentCode` | string | **Yes** | Cost component code (OKTZ structure) | Stable key for mapping table |
| `sapComponentName` | string | No | Component description | Display only |
| `amount` | decimal | **Yes** | Component amount in estimate currency | Same currency as header |

### Example components

```json
"components": [
  { "sapComponentCode": "ZMAT", "sapComponentName": "Material", "amount": "70.000000" },
  { "sapComponentCode": "ZPRC", "sapComponentName": "Production", "amount": "50.000000" }
]
```

**CPI rule:** Prefer **cost component split** from GETDETAIL, not free-text itemization lines, so codes stay mappable.

---

## 4. Full sample request

```http
POST /api/v1/erp/sap/cost-estimates
Authorization: Bearer <tenant JWT>
Content-Type: application/json
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
  "rawPayload": null,
  "components": [
    { "sapComponentCode": "ZMAT", "sapComponentName": "Material", "amount": "70.000000" },
    { "sapComponentCode": "ZPRC", "sapComponentName": "Production", "amount": "50.000000" },
    { "sapComponentCode": "ZOHD", "sapComponentName": "Overhead", "amount": "0.000000" }
  ]
}
```

---

## 5. Post-land mapping (inside our product)

Not done by CPI — done in our app:

| Step | API / UI |
|------|----------|
| Store SAP codes as received | Automatic on POST |
| Map SAP code → our code | `PUT /api/v1/erp/sap/component-mappings` or UI **SAP Cost Estimates** |
| Refresh mapped columns | `POST .../map-components` |
| Compare to should-cost | `POST .../compare-should-cost` with `shouldCostBuildUpId` |

### Our target codes (compare engine)

| Our code | Should-cost field |
|----------|-------------------|
| MATERIAL | materialCost |
| PROCESS | processCost |
| TOOLING | toolingCost |
| PACKAGING_FREIGHT | packagingFreightCost |
| OVERHEAD | overheadCost |
| SGA | sgaCost |
| PROFIT_MARGIN | profitMargin |

Unmapped SAP components remain visible on the staging row but do not enter the mapped variance matrix (those codes show **DATA NOT AVAILABLE** on the SAP side of compare when not mapped).

---

## 6. Suggested SAP → our mapping (starter only)

Customer OKTZ codes differ. Starter examples:

| Example SAP component | Map to our code |
|----------------------|-----------------|
| Material / raw material | MATERIAL |
| Production / labor / machine | PROCESS |
| Tooling / special production | TOOLING |
| Packaging / freight | PACKAGING_FREIGHT |
| Overhead / production OH | OVERHEAD |
| Admin / SG&A | SGA |
| Profit / margin | PROFIT_MARGIN |

---

## 7. CPI implementation checklist

- [ ] RFC/OData credentials in CPI secure store  
- [ ] Cloud Connector if on-prem  
- [ ] Select released / relevant cost estimates only (agree filter with CO)  
- [ ] Map header fields §2  
- [ ] Map component split §3  
- [ ] Decimal format: plain number or string, no `1,234.56` locale commas  
- [ ] HTTPS receiver → our base URL + `/api/v1/erp/sap/cost-estimates`  
- [ ] Auth: service user JWT (or agreed pattern with platform team)  
- [ ] Retry 5xx; log 4xx body  
- [ ] Schedule or event on estimate release  

---

## 8. Error handling (our API)

| Situation | Expected behaviour |
|-----------|-------------------|
| Missing `sapMaterial` or `sapPlant` | 4xx validation error |
| Empty components | Allowed; status may stay LANDED |
| Component without code or amount | 4xx |
| Unknown mapping | Component stored; `mappedComponentCode` null until mapping exists |

---

## 9. Out of scope for this mapping

- Writing cost estimates back into SAP  
- Material Ledger / actual costing  
- Auto-linking SAP material to BOM node (user selects should-cost build-up for compare)  
- Multi-currency conversion inside land (send estimate currency as-is)

---

## 10. Related docs

| Doc | Use |
|-----|-----|
| SAP_CPI_INTEGRATION_CHECKLIST.md | Process / acceptance |
| FEATURE_SAP_CO_INTEGRATION.md | Product feature |
| P2-SAP_CO_INTEGRATION_DESIGN.md | Design |
| API_SPEC.md | API reference |

---

*End of CPI field mapping sheet*
