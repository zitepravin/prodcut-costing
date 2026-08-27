# FEATURE P3-05 — Order Tracking (read-only CPI)

**Authorization:** Scope provided 2026-08-15  
**Package:** MASTER-V181  
**Module:** PURCHASE

## Scope
After a Purchase Request is **RELEASED**, show read-only order tracking status fields ingested from CPI (ASN date, qty shipped). **No SAP writes.** Human-only corrections (reason required).

## Behavior
| Action | Rule |
|--------|------|
| CPI ingest | `POST /api/v1/erp/cpi/purchase-tracking` — PR must be RELEASED |
| List | `GET /api/v1/purchase-requests/{id}/tracking` |
| Human correct | `POST .../tracking/{trackingId}/correct` — `correctionReason` required |
| data_status | `AVAILABLE` only when both asn_date and qty_shipped present; else `NOT_AVAILABLE` |
| SAP | **Never** posts to SAP MM |

## Schema
Flyway **V99** `purchase_order_tracking`

## Out of scope
- GR/IR full logistics
- Auto-correction by AI
- Creating ERP POs from this service
