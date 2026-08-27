# V292 Role-wise Dashboard — Test Report

**Date:** 2026-08-23

## Unit tests added/updated (source)

| Test | Intent |
|------|--------|
| `dashboardPersona.test.ts` | 16 business roles map to personas; APPROVAL_OVERRIDE has no persona; SYSADMIN wins over engineer; buyer ≠ purchasing manager |
| `permissionMatrix.test.ts` | Commodity buyer RFQ vs BOM; purchasing manager approve vs buyer; override grants nothing |
| `CustomerAdminBoundaryTest` | New operational roles assignable; APPROVAL_OVERRIDE not assignable |
| `DashboardServiceVendorKpiTest` | Support counts zero without SUPPORT_TICKET_READ (existing) |

## Role checks (static, from persona catalog + matrix)

| Role | Dashboard loads (persona) | Widgets | Unauthorized hidden | Quick actions | Links to existing routes | Tenant-safe API |
|------|---------------------------|---------|---------------------|---------------|--------------------------|-----------------|
| SYSTEM_ADMINISTRATOR | SYSTEM_ADMINISTRATION | Platform + costing + support | Yes | Users/Roles/Permissions/Customers/Audit | Yes | KPI customerId |
| MASTER_DATA_ADMIN | MASTER_DATA | Materials/BOM | Yes | Master data pages | Yes | Yes |
| COSTING_ENGINEER | COST_ENGINEERING | BOM/should-cost | Yes | Create costing/BOM/routing | Yes | Yes |
| COSTING_MANAGER | COST_MANAGEMENT | Variance/approvals | Yes | Review/approve | Yes | Yes |
| FINANCE_CONTROLLER | FINANCE | Approvals + finance links | Yes | Currency/overhead/machines | Yes | Yes |
| PURCHASING_BUYER | PURCHASING_OPERATIONS | Vendor/approvals | Yes | Create RFQ / PO | Yes | Yes |
| SOURCING_LEAD | SOURCING_MANAGEMENT | Vendor KPIs | Yes | Compare suppliers | Yes | Yes |
| PROGRAM_COST_MANAGER | PROGRAM_COSTING | Program/target/variance | Yes | Target / make-vs-buy | Yes | Yes |
| PROCESS_ENGINEER | PROCESS_ENGINEERING | BOM/process | Yes | Routing | Yes | Yes |
| TOOLING_ENGINEER | TOOLING | Tooling workspace | Yes | Tooling estimate | Yes | Yes |
| COMMODITY_MATERIAL_BUYER | MATERIAL_PROCUREMENT | Materials | Yes | Material RFQ | Yes | Yes |
| INDUSTRIAL_ENGINEER | INDUSTRIAL_ENGINEERING | Routing/BOM | Yes | Routing | Yes | Yes |
| PRODUCT_COST_ENGINEER | PRODUCT_COSTING | Should-cost/BOM | Yes | Should-cost / compare | Yes | Yes |
| PURCHASING_MANAGER | PURCHASING_MANAGEMENT | Approvals/vendor | Yes | Approve purchase | Yes | Yes |
| PLATFORM_OWNER | PLATFORM_ADMINISTRATION | Platform + support | Costing widgets omitted | Customers/modules/billing | Yes | Platform APIs |
| CUSTOMER_ADMIN | CUSTOMER_ADMINISTRATION | Tenant admin + support | Other tenants never in API | Users/roles/support | Yes | Tenant only |
| APPROVAL_OVERRIDE | OPERATIONS fallback only | No override KPI | N/A | None for override | ApprovalsPage | Workflow |

## Execution on this host

| Gate | Result |
|------|--------|
| Vitest / Maven against V292 | **NOT EXECUTED** (Maven not in App Builder; V292 frontend tests live in the master package) |
| Live Docker stack / real login per role | **BLOCKED** |
| Preview persona explorer | Implemented in this workspace (role switcher, 0 / No data available) |

Live verification of HTTP 403 tenant isolation remains **BLOCKED** without the Java stack.
