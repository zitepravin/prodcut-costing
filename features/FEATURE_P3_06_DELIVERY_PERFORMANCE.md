# FEATURE P3-06 — Delivery Performance (Option A)

**Package:** MASTER-V182  
**Module:** PURCHASE

## Scope
Compute delivery performance from **order tracking + PR lines** after RELEASED.

## Metrics
- delayDays = ASN − need-by (null if either missing)
- onTimeComputed = delayDays ≤ 0 when computable
- onTimeEffective = human override if set, else computed
- qtyVariance = qtyShipped − qtyOrdered (NOT_AVAILABLE if either missing)

## API
- `GET /api/v1/purchase-requests/{id}/delivery-performance`
- `POST /api/v1/purchase-requests/{id}/tracking/{trackingId}/on-time-override` (reason required)

## Schema
Flyway **V100** — on_time_override* columns on `purchase_order_tracking`

## Policy
- Missing dates/qty → DATA NOT AVAILABLE (never invent)
- Human override only; AI must not auto-penalize or auto-block
- No SAP MM writes
