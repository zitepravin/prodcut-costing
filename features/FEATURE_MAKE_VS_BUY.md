# FEATURE_MAKE_VS_BUY.md

**Feature:** Make vs Buy Simulation (P2-04) + Wave A UX/TCO + Wave B export  
**Version:** MASTER-V175  
**Module:** Product Costing  
**Status:** Implemented  

## Purpose
Side-by-side MAKE / BUY / MIXED with draft recommendation and human decision.

## Wave A (V171)
- **P3-03:** Source pickers — should-cost scenarios + accepted quotes (no raw UUID required)
- **P3-02:** BUY legs attach latest **complete** Vendor Selection TCO for supplier+BOM when available (`tcoStatus=AVAILABLE`)

## Wave B (V174–V175)
- **P3-10:** FX align leg unit costs to reporting currency (missing rate → NOT_AVAILABLE)
- **P3-04:** GET export-package (read-only JSON); FE **Export package** button (V175)

## Policy
- Draft recommendation only; NOT_ELIGIBLE cannot win
- TCO ranking when all preferred BUY have TCO; else unit cost
- Apply to BOM only after DECIDED
- Export does not post to SAP CO

## APIs
POST/GET `/api/v1/bom-nodes/{id}/make-vs-buy`  
POST `.../compare` | `decide` | `apply-to-bom`  
GET `/api/v1/make-vs-buy/{id}/export-package`

## UI
`/make-vs-buy`
