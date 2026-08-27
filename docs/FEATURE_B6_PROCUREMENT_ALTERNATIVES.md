# B6 — Procurement Alternatives

**Package:** MASTER V214

## Purpose
First-class alternative set on a BOM node:
MAKE | BUY | SUBCONTRACT | EXTERNAL_PROCESS | MIXED

Extends existing make-vs-buy / mixed / quotes without replacing them.

## Behavior
1. **Evaluate** pulls live data:
   - MAKE ← latest should-cost
   - BUY ← supplier quotes (ACCEPTED preferred)
   - SUBCONTRACT / EXTERNAL ← optional manual unit costs
   - MIXED ← latest mixed-cost blended unit total
2. Rank lines with unit cost (ascending)
3. Draft recommendation = lowest unit cost among OK lines
4. Explicitly **not** an automatic award — human **decide** required

## API
- `POST /api/v1/procurement-alternatives/evaluate`
- `POST /api/v1/procurement-alternatives/{setId}/decide`
- `GET /api/v1/procurement-alternatives?bomNodeId=`

## UI
Product Costing → Procurement Alternatives (`/procurement-alternatives`)
