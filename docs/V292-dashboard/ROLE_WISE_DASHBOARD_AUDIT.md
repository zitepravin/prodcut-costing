# V292 Role-wise Dashboard Audit

**Package:** PRODUCT-COSTING-SOFTWARE-MASTER-V292-FINAL  
**Date:** 2026-08-23  
**Method:** Source inspection of frontend dashboard, backend KPI API, permission matrix, role seeds, and routes. No guessed widgets.

## Shared shell (already existed)

| Layer | Location | Status |
|-------|----------|--------|
| Customer home | `frontend/src/pages/DashboardPage.tsx` | COMPLETE (one page) |
| Platform home | `frontend/src/pages/platform/PlatformDashboardPage.tsx` | COMPLETE (platform counts) |
| KPI API | `GET /api/v1/dashboard/kpis` | COMPLETE, tenant-scoped |
| Program cost API | `GET /api/v1/programs/{id}/cost-position` | COMPLETE |
| Permission UI | `permissionMatrix.ts` + `useAuth` | PARTIAL (missing 4 roles) |
| Module entitlement | `hasModule` + `roleAccess.ts` | COMPLETE |
| Tenant | `TenantAccessGuard.requireCustomerId()` | COMPLETE |
| Support KPIs | DashboardService + SUPPORT_TICKET_READ gate | COMPLETE |

**Classification of the previous dashboard:** one generic “Management dashboard” with permission-gated tiles. **Not** 16 role personas. APPROVAL_OVERRIDE already has workflow UI (justification), not a dashboard.

## Role table

| Role | Existing Dashboard | Existing Widgets | Existing API | Existing Permissions | Existing Entitlement | Missing |
| ---- | ------------------ | ---------------- | ------------ | -------------------- | -------------------- | ------- |
| SYSTEM_ADMINISTRATOR | PARTIAL — same home as all users; `/platform` is a separate page | Generic costing KPIs + platform page (customers/users/roles/modules) | KPI + platform list APIs | COMPLETE in backend matrix | hasModule bypass for ADMIN/SYSADMIN | Persona title, admin quick actions on home |
| MASTER_DATA_ADMIN | PARTIAL — generic home | Materials / BOM counts if COSTING_VIEW | KPI materialMasters, bomNodes | COMPLETE | PRODUCT_COSTING paths | Master-data persona, master-data quick actions |
| COSTING_ENGINEER | PARTIAL | BOM, should-cost, programs, packaging | KPI + program position | COMPLETE | PRODUCT_COSTING | Engineer persona, BOM/routing quick actions |
| COSTING_MANAGER | PARTIAL | Variance, approvals, should-cost | KPI pendingApprovalSteps, variances | COMPLETE (COSTING_APPROVE) | PRODUCT_COSTING | Manager persona, review/approve actions |
| FINANCE_CONTROLLER | PARTIAL | Approvals if present | KPI approvals; currency/machines are **pages**, not KPI counts | COMPLETE | PRODUCT_COSTING | Finance persona; **no rate-count API** (do not invent) |
| PURCHASING_BUYER | PARTIAL | Vendor widgets if VENDOR_SELECTION; RFQ is a **page**, no RFQ count API | KPI vendor fields; **no RFQ open-count** | COMPLETE | PRODUCT_COSTING + PURCHASE + VENDOR_SELECTION | Buyer persona; RFQ counts **MISSING API — not faked** |
| SOURCING_LEAD | PARTIAL | Vendor recs/risks/warranty/caps | Vendor KPI fields | COMPLETE | VENDOR_SELECTION | Sourcing persona, compare-suppliers actions |
| PROGRAM_COST_MANAGER | PARTIAL | Program cost / target / variance tiles | Program cost-position | COMPLETE | PRODUCT_COSTING | Program persona; honest NOT AVAILABLE totals |
| PROCESS_ENGINEER | PARTIAL | BOM / should-cost | KPI bomNodes | COMPLETE | PRODUCT_COSTING | Process persona; **no routing-count API** |
| TOOLING_ENGINEER | PARTIAL | Should-cost / BOM | KPI shouldCost | COMPLETE | PRODUCT_COSTING | Tooling persona; **no tooling-count API** |
| COMMODITY_MATERIAL_BUYER | MISSING persona; role exists in Flyway V4 | None dedicated | Material KPI only | MISSING from FE/BE matrix (now added) | PRODUCT_COSTING | Role grants + material-procurement persona |
| INDUSTRIAL_ENGINEER | MISSING persona; role exists in Flyway V5 | None dedicated | BOM KPI | MISSING from FE/BE matrix (now added) | PRODUCT_COSTING | Role grants + industrial persona |
| PRODUCT_COST_ENGINEER | PARTIAL — FE grants existed, BE matrix missing | Should-cost / BOM | KPI | FE PARTIAL / BE MISSING (now added) | PRODUCT_COSTING | Backend grants + product-costing persona |
| APPROVAL_OVERRIDE | COMPLETE as **workflow permission**, not a dashboard | ApprovalsPage override justification | Approval workflow | Role in V10; no dashboard grants | N/A | **No dashboard** (by design) |
| PURCHASING_MANAGER | MISSING persona; role exists in Flyway V10 | None dedicated | Approvals KPI | MISSING from FE/BE matrix (now added) | PURCHASE / RFQ | Role grants + purchasing-management persona |
| PLATFORM_OWNER | PARTIAL — `/platform` page; home was generic costing | Platform tiles | Platform list APIs | COMPLETE (control-plane perms) | No costing module bypass | Platform persona on home (no business leak) |
| CUSTOMER_ADMIN | PARTIAL — `/customer-admin/*` pages; home generic | Support KPIs | Support + customer-admin APIs | COMPLETE | Tenant membership | Customer-admin persona; tenant-only actions |

## Legend

- **COMPLETE** — exists and is usable for that role’s job.
- **PARTIAL** — shared dashboard/API exists but is not role-specific.
- **MISSING** — role or persona not wired into dashboard/matrix.

## APIs that do **not** exist (must not be faked)

| Desired widget | Reality |
|----------------|---------|
| Open RFQ / closing-soon counts | No `countByCustomer` on RFQ repository |
| Incomplete master-data counts | No incomplete-master query |
| Currency/overhead/machine rate change counts | Pages exist (`/currency`, `/machines`, `/financial`); no dashboard aggregates |
| Routing / cycle-time / line-balance counts | Process pages exist; no dashboard aggregates |
| Tooling investment totals | Tooling page exists; no dashboard aggregate |
| Buyer workload / purchasing spend | No spend API on dashboard |

Those surfaces are linked as **workspaces / quick actions**, with KPI value `0` / `No data available` when the live count is zero — never invented statistics.
