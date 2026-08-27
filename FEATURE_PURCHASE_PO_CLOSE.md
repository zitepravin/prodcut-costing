# Purchase — Light PO close / partial receive (MASTER-V245)

## Status machine
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

## Permanent non-goals (enforced)
| Constraint | Enforcement |
|------------|-------------|
| Not ERP GR | Status change + optional note only; no GR entity, no stock qty post |
| Not IR | No invoice / 3-way match |
| Not SAP MM | No outbound SAP post; issue message states export/reference only |
| No invented qty/price | Issue requires all lines `AVAILABLE`; close does not alter line qty/price |

## FE
**Partial receive** / **Close PO** on Purchase Orders page

## Tests
16 PASS (status + lifecycle + PR→PO)
