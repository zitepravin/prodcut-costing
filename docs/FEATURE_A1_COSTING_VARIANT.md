# A1 — Costing Variant Engine

**Status:** Implemented in MASTER V208  
**Purpose:** Reusable SAP-style configuration that controls how a cost estimate is parameterized.

## Rules stored on variant

- Valuation strategy (STANDARD / PLANNED / CURRENT / SHOULD_COST)
- Material price source, activity price source
- Quantity-structure source (BOM_AND_ROUTING / BOM_ONLY / MANUAL / PARAMETRIC)
- Currency, lot size, scrap %, yield %
- Optional overhead pool code, cost component profile, costing sheet code (sheet engine still P0 A6)
- Validity dates, status DRAFT → ACTIVE → RETIRED

## API

- `GET/POST /api/v1/costing-variants`
- `PUT /api/v1/costing-variants/{id}`
- `POST .../activate` | `.../retire`
- Should-cost `GenerateRequest.costingVariantId` optional — ACTIVE variant required; supplies OH pool default and basis metadata

## Non-goals

- Does not calculate money itself
- Does not replace ShouldCostService
- Does not implement full costing sheet (A6) or mass run (B1)
