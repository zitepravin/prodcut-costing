# FEATURE P3-01 Hardening C / E / F / G

**Authorization:** AUTHORIZE P3-01 HARDENING Scope: C, E, F, G  
**Package:** MASTER-V179  
**Date:** 2026-08-15

## C — TCO snapshot
- On line create, load latest `SupplierTcoCalculation` for supplier + BOM where `dataComplete=true` and `tcoTotal` present.
- If `annualVolume > 0`, store unit = total / volume; else store total as `tco_unit`.
- Incomplete or missing → `tco_status = NOT_AVAILABLE`, `tco_unit = null` (never invent zero).

## E — Submit workflow / notification
- On SUBMIT: attempt `ApprovalWorkflowService.submit` for entityType `PURCHASE_REQUEST` when a definition exists.
- Always publish in-app notification `PURCHASE_REQUEST_SUBMITTED` to submitter when user id known.
- If no workflow definition: PR stays SUBMITTED; human decide API remains the path (no auto-approve).

## F — Nav entitlement
- `AppLayout` hides `/purchase-requests` unless `hasModule('PURCHASE')`.
- Backend `@RequiresModule("PURCHASE")` remains the security boundary.

## G — Expanded tests
- `PurchaseServiceStatusTest`: APPROVED gate, BLOCKED fail, TCO complete/incomplete/missing, constants, currentUserId.

## Unchanged policy
- No AI approve/release
- No invented FX / no SAP MM post
- No RELEASE on NOT_AVAILABLE without product change
