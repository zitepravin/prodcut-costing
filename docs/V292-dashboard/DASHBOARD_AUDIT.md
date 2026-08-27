# V292 Dashboard — Audit

**Package:** PRODUCT-COSTING-SOFTWARE-MASTER-V292-FINAL  
**Date:** 2026-08-23

## Existing implementation (pre-change)

### Backend
| Component | Path | Notes |
|-----------|------|-------|
| Controller | `dashboard/controller/DashboardController.java` | `GET /api/v1/dashboard/kpis` only |
| Service | `dashboard/service/DashboardService.java` | Tenant-scoped counts via `TenantAccessGuard.requireCustomerId()` |
| DTOs | `dashboard/dto/DashboardDtos.java` | `KpiResponse` + program cost position types |
| Program cost | `ProgramCostPositionController` + engine | Separate program cost-position API |
| Indexes | `V89__dashboard_vendor_kpi_support_indexes.sql` | KPI query support |
| Tests | `DashboardServiceVendorKpiTest` | Mockito unit test for vendor KPIs |

### Frontend
| Component | Path | Notes |
|-----------|------|-------|
| Customer home | `pages/DashboardPage.tsx` | KPI tiles, actions, cost workspaces, quick actions |
| Platform | `pages/platform/PlatformDashboardPage.tsx` | Customers / modules / users / roles counts |
| API client | `api/dashboardApi.ts` | `fetchDashboardKpis`, `fetchProgramCostPosition` |
| Routes | `App.tsx` index → `DashboardPage`; `/platform` → platform dashboard |
| Nav | `layout/AppLayout.tsx` | Dashboard always in sidebar |

### Security model already in platform
- `PermissionAuth` + `@permissionAuth.has('CODE')`
- Frontend `useAuth().hasPermission` / `hasModule` / `hasRole`
- `PERMISSIONS` matrix + effective access from `/api/v1/me/access`
- Module codes: `PRODUCT_COSTING`, `VENDOR_SELECTION`, `PURCHASE`, etc.

### Gaps found
1. **DashboardPage did not use `hasPermission` / `hasModule`** — all KPI tiles and most quick actions visible to any authenticated user.
2. **No Support Ticket KPIs** on dashboard despite V162/V163 module.
3. **Backend KPI endpoint** did not zero/suppress support-ticket counts by permission (now fixed).
4. **No consolidated permission matrix document** for dashboard widgets (now produced).
5. Live stack verification blocked (no Docker/Maven on host).

### What was already correct
- Tenant isolation on KPI counts (`countByCustomerId`)
- Honest "NOT AVAILABLE" for program monetary totals when no program selected
- Loading / error / empty handling patterns present
- Platform dashboard separated from customer dashboard
