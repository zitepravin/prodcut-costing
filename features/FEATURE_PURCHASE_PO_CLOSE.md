# Purchase — Light PO close / partial receive (MASTER-V245)

## Status machine extension
```
ISSUED → PARTIALLY_RECEIVED | CLOSED | CANCELLED
PARTIALLY_RECEIVED → CLOSED | CANCELLED
CLOSED (terminal)
```

## API
`POST /api/v1/purchase-orders/{id}/close`
```json
{ "reason": "optional text", "partial": true|false }
```
- `partial: true` → PARTIALLY_RECEIVED (from ISSUED only)
- `partial: false` → CLOSED (from ISSUED or PARTIALLY_RECEIVED)

## Explicit non-goals
- Not ERP goods receipt (GR)
- Not invoice receipt (IR)
- Not SAP MM posting
- No invented quantities / prices

## FE
Purchase Orders detail: **Partial receive** / **Close PO**

## Tests
PurchaseOrderStatusTest 9 · Lifecycle (+close) 4 · FromPr 3 = **16 PASS**
