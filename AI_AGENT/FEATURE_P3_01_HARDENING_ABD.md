# P3-01 Hardening A / B / D (MASTER-V178)

**Authorization:** AUTHORIZE P3-01 HARDENING Scope: A, B, D  
**Date:** 2026-08-15

## A — Qualification APPROVED gate
- On line create, submit, approve, and release: supplier must have `supplier_qualification_current.lifecycle_status = APPROVED`.
- Fail closed if missing qualification, BLOCKED, SUSPENDED, or any other status.
- Independent of supplier_master.ACTIVE (BLOCKED master still rejected).

## B — Quote integrity
- Quote must exist.
- `quote.bom_node_id` must equal purchase request BOM node.
- `quote.supplier_id` must equal line supplier.
- Status must be ACCEPTED unless header `manualOverrideReason` is present (audited note on line).

## D — Actor identity
- `decide` sets `decided_by` + `decided_at` from `SecurityContext` principal.
- `release` sets `released_by` + `released_at` (Flyway V98 adds `released_by`).

## Unchanged policy
- No AI approve/release
- No SAP MM post from export
- RELEASE still blocked when any line `dataStatus=NOT_AVAILABLE`
