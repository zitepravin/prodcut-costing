# V292 Dashboard Final Manifest

**Date:** 2026-08-23  
**Master:** PRODUCT-COSTING-SOFTWARE-MASTER-V292  
**Scope:** Role-wise dashboard implementation **in the V292 master** (not a side package)

## Dashboard backend files changed

- `backend/src/main/java/com/automotive/costing/dashboard/dto/DashboardDtos.java`
- `backend/src/main/java/com/automotive/costing/dashboard/service/DashboardService.java`
- `backend/src/main/java/com/automotive/costing/dashboard/controller/DashboardController.java`
- `backend/src/main/java/com/automotive/costing/dashboard/controller/ProgramCostPositionController.java`
- `backend/src/main/java/com/automotive/costing/rfq/repository/RfqEventRepository.java`
- `backend/src/main/java/com/automotive/costing/purchase/repository/PurchaseOrderRepository.java`
- `backend/src/main/java/com/automotive/costing/purchase/repository/PurchaseOrderLineRepository.java`
- `backend/src/main/java/com/automotive/costing/purchase/repository/PurchaseRequestRepository.java`
- `backend/src/main/java/com/automotive/costing/process/repository/ProcessRoutingRepository.java`
- `backend/src/main/java/com/automotive/costing/process/repository/ProcessOperationRepository.java`
- `backend/src/main/java/com/automotive/costing/tooling/repository/ToolingRecordRepository.java`
- `backend/src/main/java/com/automotive/costing/masterdata/repository/MaterialMasterRepository.java`

## Dashboard frontend files changed

- `frontend/src/dashboard/dashboardPersona.ts`
- `frontend/src/dashboard/dashboardPersona.test.ts`
- `frontend/src/api/dashboardApi.ts`
- `frontend/src/pages/DashboardPage.tsx`

## Dashboard APIs added/changed

| Method | Path | AuthZ | Tenant |
|--------|------|-------|--------|
| GET | `/api/v1/dashboard/kpis` | `@permissionAuth.has('DASHBOARD_VIEW')` + slice permissions | `TenantAccessGuard.requireCustomerId()` |
| GET | `/api/v1/programs/{id}/cost-position` | COSTING_VIEW or SHOULD_COST; `@RequiresModule("PRODUCT_COSTING")` | program ownership (existing service) |

New KPI fields (0 when permission denied; never invented):

`openRfqs`, `rfqsClosingSoon`, `awaitingQuotations`, `openPurchaseOrders`, `overduePurchaseOrders`, `openPurchaseRequests`, `buyerOpenOrders`, `purchasingSpend`, `incompleteMaterialMasters`, `draftRoutings`, `routingMissingAssignments`, `cycleTimeExceptions`, `activeToolingRecords`, `toolingInvestment`

## Database migrations added

- `V164__dashboard_operational_kpis_and_roles.sql`
  - Seeds `COMMODITY_MATERIAL_BUYER`, `INDUSTRIAL_ENGINEER`, `PRODUCT_COST_ENGINEER`
  - Idempotent indexes on RFQ, PO, PR, routing, operation, tooling, material

## Permissions added/changed

- No new permission codes.
- Dashboard endpoint now requires existing catalog permission `DASHBOARD_VIEW`.
- RFQ/PO slices: `RFQ_VIEW`
- Engineering slices: `COSTING_VIEW`
- Support slices: `SUPPORT_TICKET_READ` (unchanged)

## Role personas

16 business roles mapped in `dashboardPersona.ts`. `APPROVAL_OVERRIDE` is excluded (fallback OPERATIONS). `ADMIN` aliases SYSTEM_ADMINISTRATION.

## Customer entitlement behavior

- UI: `hasModule` + `hasPermission` + optional role list on widgets/actions.
- PO tiles also require `PURCHASE` module.
- Backend KPI payload is one document; unauthorized slices are **0**, not omitted fabricated values.
- Program cost-position blocked when PRODUCT_COSTING module is off (`@RequiresModule`).

## Tenant isolation

- All new counts/sums take `customerId` from `TenantAccessGuard.requireCustomerId()`.
- Buyer workload additionally scoped to current user id.

## Tests

### Backend
- `DashboardServiceVendorKpiTest` (vendor + support + RFQ/PO + engineering + tenant id + permission skip)
- `DashboardControllerTest`
- `DashboardPermissionTest`
- `DashboardEntitlementTest`
- `DashboardTenantIsolationTest`
- `DashboardAggregationTest`

### Frontend
- `dashboardPersona.test.ts`
- `DashboardRole.test.ts`
- `DashboardPermission.test.ts`
- `DashboardEntitlement.test.ts`
- `DashboardPage.test.tsx`

## Build / test result (this host)

| Check | Result |
|-------|--------|
| Frontend vitest (full suite) | **PASS** — 14 files, 85 tests |
| Frontend typecheck | Pre-existing errors in BomPage / PlatformConfiguration / Support pages (not dashboard files) |
| Backend `mvn test` | **BLOCKED** — JDK 21 and Maven not installed on this host (OpenJDK 17 only) |
| Live dual-tenant HTTP | **PENDING** |

## Remaining gaps

- Live per-role login against Docker/MySQL not executed here.
- Purchasing spend is sum of `unit_price * quantity` for ISSUED/PARTIALLY_RECEIVED/CLOSED POs in tenant currency mix (no FX conversion on the dashboard tile).
- Platform/Customer Admin user counts remain navigation tiles (no dedicated user-count API added).
- Application-wide `@PreAuthorize` → permissionAuth migration outside dashboard is unchanged.
