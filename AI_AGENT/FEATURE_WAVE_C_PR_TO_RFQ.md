# Wave C — PR → existing RFQ (MASTER-V235)

## Decision D2 (implementation default)
- PR → RFQ is exposed as **PURCHASE** API: `POST /api/v1/purchase-requests/{id}/create-rfq`
- Reuses **existing** `RfqService.create` + `addLine` (no second RFQ entity)
- Subsequent open RFQ / portal grant remain on **PRODUCT_COSTING** RFQ APIs
- Purchase-only tenants can **create** RFQ draft from PR; inviting suppliers still needs buyer RFQ tools (typically PRODUCT_COSTING)

## Rules
- Only from status **APPROVED**
- Transitions to **RFQ_LINKED**
- One linked RFQ per PR (`linked_rfq_id`)
- Maps program, required date, currency, plant/purpose in notes; lines use PR BOM node + qty/UOM/target price

## Unchanged
- No auto-award, no invent FX, module gates intact
