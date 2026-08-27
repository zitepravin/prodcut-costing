# Buyer quote vs should-cost + Seller portal clarification/attachments (MASTER-V234)

## 1. Buyer quote comparison vs should-cost
- API: `GET /api/v1/rfq/events/{rfqId}/quote-comparison`
- Module: PRODUCT_COSTING (existing RFQ controller)
- For each quote: latest should-cost `totalCost` on BOM node
- `comparisonStatus` AVAILABLE | NOT_AVAILABLE (no invented FX)
- Variance = quote unit price − should-cost total
- FE: RFQ page table “Quote vs Should-Cost”
- Message: lowest price ≠ best supplier

## 2. Seller portal clarification + attachments
- Flyway **V125**: `rfq_clarification_request`, `rfq_attachment` (metadata URI only)
- Seller (portal JWT, granted RFQ only):
  - `POST /api/v1/portal/clarifications`
  - `GET /api/v1/portal/clarifications?rfqId=`
  - `GET /api/v1/portal/rfqs/{rfqId}/attachments`
- Buyer:
  - list/respond clarifications
  - register attachment metadata (`storageUri` to external/object store)
- No competitor data leak; supplierId from token only

## Out of scope
- Binary file upload service
- Wave C PR→RFQ
- Auto-award
