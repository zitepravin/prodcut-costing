# A2 — Automatic Quantity-Structure Costing

**Status:** Implemented in MASTER V209

## Flow

```text
BOM node
  + Process routing (operations)
  + Optional Costing Variant (lot, scrap, yield)
  + Material master / Machine / Labor rates
        ↓
MaterialCostEngine + ProcessCostEngine
        ↓
Quantity-structure estimate (itemized operations + material)
```

## API

`POST /api/v1/quantity-structure/estimate`

Body: `bomNodeId` (required), `costingVariantId`, `routingId`, `lotSize` (optional overrides)

## Rules

- Reuses existing engines — **no second costing engine**
- Variant must be **ACTIVE** when referenced
- Lot size amortizes setup cost per piece across operations with setup time
- Scrap/yield from variant (or BOM node scrap) applied to material gross weight
- Missing material rate → line with `MATERIAL_RATE_UNAVAILABLE` (no invented price)

## UI

Product Costing → Quantity Structure (`/quantity-structure`)
