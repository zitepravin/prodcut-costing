# V292 Role-wise Dashboard Matrix

**Date:** 2026-08-23

| Role | Existing | Partial | Missing | Action |
| ------------------------ | -------- | ------- | ------- | ------ |
| SYSTEM_ADMINISTRATOR | Platform page, KPI API, tenant isolation | Generic home title/actions | Persona + admin quick actions on home | EXTEND |
| MASTER_DATA_ADMIN | Materials/BOM KPI, master-data pages | Generic home | Master-data persona | EXTEND |
| COSTING_ENGINEER | Costing KPIs, BOM/should-cost routes | Generic home | Engineer persona + create BOM/routing actions | EXTEND |
| COSTING_MANAGER | Approvals + variance KPIs | Generic home | Manager persona + review/approve actions | EXTEND |
| FINANCE_CONTROLLER | Approvals KPI, currency/finance pages | Generic home; no rate KPIs | Finance persona; link existing pages | EXTEND |
| PURCHASING_BUYER | RFQ/PO/vendor pages; vendor KPIs | Generic home; no RFQ counts | Buyer persona; RFQ as workspace not fake KPI | EXTEND |
| SOURCING_LEAD | Vendor KPIs + vendor-selection | Generic home | Sourcing persona | EXTEND |
| PROGRAM_COST_MANAGER | Program cost-position API | Generic home | Program persona | EXTEND |
| PROCESS_ENGINEER | Process page, BOM KPI | Generic home | Process persona | EXTEND |
| TOOLING_ENGINEER | Tooling page | Generic home | Tooling persona | EXTEND |
| COMMODITY_MATERIAL_BUYER | Role in DB (V4) | — | FE/BE grants + persona | IMPLEMENT grants + EXTEND dashboard |
| INDUSTRIAL_ENGINEER | Role in DB (V5) | — | FE/BE grants + persona | IMPLEMENT grants + EXTEND dashboard |
| PRODUCT_COST_ENGINEER | FE grants | BE matrix missing | Backend grants + persona | IMPLEMENT BE + EXTEND dashboard |
| APPROVAL_OVERRIDE | Workflow + justification UI | — | Separate dashboard (must not exist) | KEEP — no dashboard |
| PURCHASING_MANAGER | Role in DB (V10) | — | FE/BE grants + persona | IMPLEMENT grants + EXTEND dashboard |
| PLATFORM_OWNER | `/platform` dashboard | Home showed costing tiles | Platform persona (no business leak) | EXTEND |
| CUSTOMER_ADMIN | Customer-admin pages, support KPIs | Generic home | Customer-admin persona | EXTEND |

**Action key:** KEEP / EXTEND / IMPLEMENT / no dashboard.
