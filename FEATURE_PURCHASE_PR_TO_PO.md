# Purchase next development — PR → PO (MASTER-V244)

## Capability
`POST /api/v1/purchase-requests/{prId}/create-purchase-order`

Body:
```json
{ "supplierId": "...", "plantCode": null, "currencyCode": null, "paymentTerms": null, "incoterms": null, "notes": null }
```

## Rules
- Module **PURCHASE** required (`@RequiresModule`)
- Tenant: PR `customer_id` must match `X-Customer-Id` (else not found)
- PR status must be **APPROVED | RFQ_LINKED | RELEASED**
- PR must have ≥1 line
- PO created as **DRAFT** with lines copied (qty, UOM, unit price, material)
- Does **not** auto-approve or auto-issue
- Issue still blocked when any line price is `NOT_AVAILABLE`

## FE
Purchase Requests page: **Create PO from PR** (supplier ID required)

## Tests
PurchaseOrderFromPrTest 3 PASS (+ lifecycle/security regression)
