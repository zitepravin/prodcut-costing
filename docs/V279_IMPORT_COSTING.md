# V279 Import Costing

Normal import only. Multi-item allocation, FX via CurrencyService, duty/tax config CRUD.

## Gaps closed vs V278 PARTIAL
1. Multi-item shipment + allocation reconciliation
2. Explicit FX (invoice ≠ target) via CurrencyService.convert + asOfDate
3. Config CRUD: duty-rules, tax-rules (+ approve)
4. FE page `/import-costing`
5. Expanded golden tests (G1,G2,G4,G5,G6)

## Still optional / next
- Freight/insurance/incoterm full config CRUD
- 1k/10k performance bench
- Live DB migration proof
