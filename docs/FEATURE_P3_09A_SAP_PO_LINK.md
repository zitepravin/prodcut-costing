# FEATURE P3-09a — SAP PO Link (CPI callback)

**Package:** MASTER-V187  
**Parent:** FEATURE_P3_09 (APPROVED)

## §11 locked
1. RELEASED only  
2. Service JWT (app auth)  
3. Human overwrite with reason allowed  
4. Header only (no line items)  
5. Gap P3-09  

## Delivered
- V103 `po_link_*` columns + `purchase_po_link_event`
- `POST /api/v1/erp/cpi/purchase-requests/{id}/sap-po-link` (CPI)
- `POST /api/v1/purchase-requests/{id}/sap-po-link` (human + reason)
- Idempotent on `sourceEventId`
- FE shows SAP PO + link status on PR detail
- **No SAP MM write**

## States
`NOT_LINKED` → `LINKED` (CPI_CALLBACK | MANUAL_ADMIN | HUMAN_RELEASE)
