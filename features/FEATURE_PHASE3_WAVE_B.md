# FEATURE_PHASE3_WAVE_B.md

**Version:** MASTER-V175  
**Date:** 2026-08-15  
**Authorization:** Phase 3 → Wave B (P3-10, P3-04) + V175 FE completion

## P3-10 FX alignment
- Make-vs-Buy: legs converted to simulation `reportingCurrency` (default base currency) via CurrencyService
- Missing FX rate → leg `dataStatus=NOT_AVAILABLE` + note (no invented rate)
- Mixed Costing: supplier quote legs converted to base currency before blend; missing rate → NOT_AVAILABLE

## P3-04 SAP decision export
- `GET /api/v1/make-vs-buy/{simulationId}/export-package` (V174)
- Read-only JSON for CPI/process teams
- Does **not** post to SAP CO
- **UI (V175):** Make-vs-Buy page → **Export package** downloads JSON file

## Out of scope
- P3-01 PO, ML, auto-approval, direct RFC
