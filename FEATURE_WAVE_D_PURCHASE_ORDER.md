# Wave D — Purchase Order (MASTER-V238)

## Goal
Independent Purchase Order document under module `PURCHASE`.

## Delivered
- Flyway **V127**: `purchase_order`, `purchase_order_line`
- Status: DRAFT → SUBMITTED → APPROVAL → APPROVED → ISSUED; REJECTED → REWORK; CANCELLED
- `PurchaseOrderService` + `PurchaseOrderController` `@RequiresModule("PURCHASE")`
- Tenant isolation via `customer_id` + `X-Customer-Id`
- Issue blocked when any line `unit_price` is `NOT_AVAILABLE` (no invented price)
- Optional link to PR (must be APPROVED / RFQ_LINKED / RELEASED) and RFQ id
- Notifications: SUBMITTED, APPROVAL_REQUIRED, APPROVED, REJECTED, ISSUED
- FE: `/purchase-orders`, nav under Purchase, hierarchical parent Purchase
- Unit: `PurchaseOrderStatusTest` 7 PASS

## Unchanged policy
- No AI auto-approve / auto-issue
- Does **not** post to SAP MM / ME21N
- No second RFQ / SupplierQuote
- Product Costing & Vendor Selection untouched

## APIs
| Method | Path |
|--------|------|
| POST | `/api/v1/purchase-orders` |
| GET | `/api/v1/purchase-orders` |
| GET | `/api/v1/purchase-orders/{id}` |
| POST | `/api/v1/purchase-orders/{id}/submit` |
| POST | `/api/v1/purchase-orders/{id}/decide` |
| POST | `/api/v1/purchase-orders/{id}/rework` |
| POST | `/api/v1/purchase-orders/{id}/issue` |
| POST | `/api/v1/purchase-orders/{id}/cancel` |
