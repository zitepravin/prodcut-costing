# C2–C4 Financial Integration

**Package:** MASTER V216

## C2 Planned / Standard / Actual
- `actual_cost_record` — **import-only** actuals (never invented)
- Triangle buckets: PLANNED (target), STANDARD (released/frozen), SHOULD, ACTUAL, SAP
- Missing bucket = `DATA_NOT_AVAILABLE`

## C3 SAP Cost Integration
- Uses existing SAP cost estimate **staging**
- Compares staged SAP total to standard / should / actual
- **No Material Ledger / FI-CO clone** (`SAP_INTEGRATION_BOUNDARY`)

## C4 Multi-currency valuation
- `valuation_currency_policy`: company / group / transaction / costing currencies
- Valuation date policy: COSTING_DATE | DOCUMENT_DATE | TODAY | FIXED_DATE
- Conversion via existing `CurrencyService` rates

## API
`/api/v1/financial/actual-costs/*`  
`/api/v1/financial/cost-triangle`  
`/api/v1/financial/valuation-policies/*`

## UI
Product Costing → Financial Integration (`/financial`)
