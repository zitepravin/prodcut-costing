# V292 Role-wise Dashboard — Implementation

**Date:** 2026-08-23

## Architecture

```
Dashboard Shell (DashboardPage)
       ↓
Role Dashboard Persona (dashboardPersona.ts)
       ↓
Permission-aware Widgets
       ↓
Customer Entitlement (hasModule)
       ↓
Tenant (KPI API customerId)
```

One React page. Sixteen business personas. APPROVAL_OVERRIDE is not a persona.

## Reused (KEEP)

- `GET /api/v1/dashboard/kpis` (tenant-scoped counts, support zeros without SUPPORT_TICKET_READ)
- Program cost-position API
- PlatformDashboardPage
- Customer-admin pages, RFQ/PO/vendor/process/tooling/currency pages
- PermissionAuth / TenantAccessGuard
- Metric tiles, loading/error/empty patterns

## Extended

| File | Change |
|------|--------|
| `frontend/src/dashboard/dashboardPersona.ts` | Persona catalog, priority, widgets, workspaces, quick actions |
| `frontend/src/pages/DashboardPage.tsx` | Persona title, gated widgets, gated workspaces, gated quick actions |
| `frontend/src/auth/permissionMatrix.ts` | COMMODITY_MATERIAL_BUYER, INDUSTRIAL_ENGINEER, PURCHASING_MANAGER, APPROVAL_OVERRIDE empty grants, DASHBOARD_VIEW |
| `PermissionMatrixService.java` | Same roles + assignable set; APPROVAL_OVERRIDE forbidden for Customer Admin |
| Tests | persona tests, matrix tests, CustomerAdminBoundaryTest |

## Not implemented (honest gaps)

No new KPI SQL for RFQ open counts, incomplete masters, rate-change counts, routing counts, tooling investment, or purchasing spend — those repositories have no dashboard aggregates. Personas link to the **existing pages** instead of inventing numbers.

## Permission rule

Widget/action renders only if:

```
persona includes widget
AND permission (if required)
AND module entitlement (if required)
AND tenant membership (API)
```

Missing permission → **do not render** (not disabled).

## APPROVAL_OVERRIDE

No dashboard KPI. Override remains on ApprovalsPage: justification → audit log.
