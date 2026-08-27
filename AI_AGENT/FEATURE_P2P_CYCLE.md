# FEATURE — End-to-end P2P cycle (Create PO center) · MASTER-V248

## Business purpose

Support a **procure-to-pay operational cycle** inside the platform:

```text
Need (BOM / program)
  → Purchase Request (PR)
  → [optional] RFQ + quote
  → Create PO (DRAFT)          ← center of this feature
  → Submit → Human approve
  → Issue (externalPoReference optional / copied from PR)
  → Partial receive / Close    ← operational end (NOT ERP GR/IR)
```

This is **not** a full ERP P2P (no GR, no IR, no SAP MM post, no invented prices).

## Module gate

All steps require **PURCHASE** entitlement + `X-Customer-Id`.

## Cycle steps (API)

| Step | API | Status result |
|------|-----|----------------|
| 1 Create PR | `POST /api/v1/bom-nodes/{bomNodeId}/purchase-requests` | DRAFT |
| 2 Submit PR | `POST /api/v1/purchase-requests/{id}/submit` | SUBMITTED |
| 3 Decide PR | `POST /api/v1/purchase-requests/{id}/decide` | APPROVED |
| 4a Optional RFQ | `POST /api/v1/purchase-requests/{id}/create-rfq` | RFQ_LINKED |
| 4b Optional release | `POST /api/v1/purchase-requests/{id}/release` | RELEASED (+ optional externalPoReference) |
| **5 Create PO from PR** | `POST /api/v1/purchase-requests/{prId}/create-purchase-order` | **PO DRAFT** (lines + externalPoReference copy) |
| 5b Or manual PO | `POST /api/v1/purchase-orders` | DRAFT |
| 6 Submit PO | `POST /api/v1/purchase-orders/{id}/submit` | APPROVAL |
| 7 Decide PO | `POST /api/v1/purchase-orders/{id}/decide` | APPROVED |
| 8 Issue PO | `POST /api/v1/purchase-orders/{id}/issue` | ISSUED |
| 9 Partial | `POST /api/v1/purchase-orders/{id}/close` `{partial:true}` | PARTIALLY_RECEIVED |
| 10 Close | `POST /api/v1/purchase-orders/{id}/close` `{partial:false}` | CLOSED |

## Create PO rules (center)

- Supplier required
- ≥1 line; quantity > 0
- Unit price missing → `dataStatus=NOT_AVAILABLE` → **cannot Issue**
- From PR: status must be APPROVED \| RFQ_LINKED \| RELEASED
- Copies PR `externalPoReference` when present (reference only)

## Permanent non-goals

See `PURCHASE_NON_GOALS.md`:
- Not GR / IR / SAP MM live post
- No invented qty or price

## FE

| Page | Role in cycle |
|------|----------------|
| Purchase Requests | PR lifecycle, Create RFQ, Create PO from PR |
| Purchase Orders | Create / Submit / Decide / Issue / Partial receive / Close |

## Verification

- Offline: `PurchaseOrderP2pCycleTest` (service state machine through CLOSED)
- Host: `./scripts/p2p-cycle-smoke.sh`
