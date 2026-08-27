# Wave B — Purchase Requisition (MASTER-V233)

## Goal
Evolve existing `purchase_request` into a controlled Purchase Requisition under `@RequiresModule("PURCHASE")`.

## Delivered
- Flyway **V124**: pr_number, plant, requester, department, buyer, required_date, priority, purpose; line material/target price/cost center; supplier_id nullable
- Status machine: DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED|REJECTED; CANCELLED; REJECTED→DRAFT; APPROVED→RELEASED
- Submit mandatory: plantCode, requiredDate, line materialCode + qty + uom
- APIs: start-review, cancel, reopen
- FE: plant/required date/priority on create; display prNumber
- Unit tests: PurchaseRequestStatusTest

## Unchanged policy
- Module PURCHASE only
- No AI approve; no invent FX; RELEASE blocked on NOT_AVAILABLE
- No second RFQ (Wave C)
- No full PO document (Wave D)

## Decision D1
**Evolve** purchase_request (not new requisition table).
