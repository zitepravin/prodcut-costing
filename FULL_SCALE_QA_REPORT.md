# FULL_SCALE_QA_REPORT.md — Module-by-module development + testing

**Date:** 2026-08-14  
**Source of truth:** PRODUCT-COSTING-SOFTWARE-MASTER-V149 → **V150**  
**Agent environment:** Linux sandbox — **no Docker**, **no route to customer Windows localhost:8080/8088/3307**

## Legend

| Status | Meaning |
|--------|---------|
| **COMPLETE** | FE page + BE package + Flyway + routes present; no approved gap found |
| **PASS** | Automated tests executed in this cycle with zero failures |
| **NOT RUN** | Requires live stack / Playwright / Docker not available on agent |
| **BLOCKED** | Environment cannot execute (Docker/live API) |
| **N/A** | Combined into another module per existing architecture |

**BLOCKED and NOT RUN are never reported as PASS.**

---

## Environment

| Check | Result |
|-------|--------|
| Backend unit `mvn -DskipITs test` | **PASS** — Tests run: **453**, Failures: **0**, Errors: **0**, Skipped: **1** (Flyway IT) |
| Frontend Vitest | **PASS** — **17/17** |
| Frontend production build (`vite build`) | **PASS** |
| Docker compose / live API / Playwright against localhost | **BLOCKED** / **NOT RUN** |
| Flyway max | **V91** (next free V92) |

---

## Module matrix (1–29)

| # | Module | FE | BE | DB/API | Dev gaps found | Offline test | Live UI/API |
|---|--------|----|----|--------|----------------|--------------|-------------|
| 1 | Authentication / MFA | LoginPage, AuthContext, MFA field | AuthController, MfaController | JWT + MFA seed | None | Unit in suite **PASS** | NOT RUN |
| 2 | Central Access Management | Platform customers/modules/users/roles/permissions | CentralAccessController, platform.* | CAM tables V8+ | None | platform tests **PASS** | NOT RUN |
| 3 | Dashboard | DashboardPage | DashboardController, DashboardService (UI-04 unread fix) | KPI aggregates | None | DashboardServiceVendorKpiTest **PASS** | NOT RUN |
| 4 | Master Data | Materials/Machines + programs via BOM | Material/Machine/Labor/VehicleProgram/Overhead + bulk import | master tables | None | masterdata tests **PASS** | NOT RUN |
| 5 | Materials | MaterialsPage | MaterialMasterController | material_master | None | MaterialMaster* **PASS** | NOT RUN |
| 6 | Machines | MachinesPage | MachineMasterController | machine_master | None | MachineMaster* **PASS** | NOT RUN |
| 7 | BOM | BomPage (UI-01 auto-load) | BomNodeController | bom_node | None | BomRollup* **PASS** | NOT RUN |
| 8 | Material Costing | (via BOM/should-cost flows) | MaterialCostController + engine | material_cost_calculation | None | MaterialCostEngine/Service **PASS** | NOT RUN |
| 9 | Process Costing | ProcessCostingPage | ProcessCost/Routing controllers | process routing | None | ProcessCost* **PASS** | NOT RUN |
| 10 | Tooling | ToolingPage | ToolingRecord/Cost controllers | tooling | None | Tooling* **PASS** | NOT RUN |
| 11 | Packaging | PackagingPage | PackagingLogisticsController | packaging profiles | None | Packaging engine **PASS** | NOT RUN |
| 12 | Logistics | **N/A** — same module as Packaging (`PackagingLogistics*`) | same | same | Not a separate approved module | same | N/A |
| 13 | Currency / FX | CurrencyPage | CurrencyController | FX rates seed | None | Currency* **PASS** | NOT RUN |
| 14 | Should-Cost | ShouldCostPage | ShouldCostController, BenchmarkController | should_cost_* | None | ShouldCost* **PASS** | NOT RUN |
| 15 | Target Costing | TargetCostingPage | TargetCostingController | target_cost_* | None | TargetCostVarianceEngine **PASS** | NOT RUN |
| 16 | RFQ | RfqPage (events tab) | RfqSupplierController | rfq_* | None | rfq tests in suite | NOT RUN |
| 17 | Supplier Quotes | RfqPage (suppliers/quotes) | same + portal | supplier_quote | None | suite | NOT RUN |
| 18 | Cost Comparison | CostComparisonPage | CostComparisonController | aggregates | None | CostComparisonServiceTest **PASS** | NOT RUN |
| 19 | Version Control | VersionControlPage | CostVersionController | cost_version | None | CostVersionServiceTest **PASS** | NOT RUN |
| 20 | Workflow / Approvals | ApprovalsPage | ApprovalWorkflowController, WorkflowDefinition | workflow_* | None | ApprovalWorkflow* **PASS** | NOT RUN |
| 21 | Notifications | NotificationsPage | NotificationController | app_notification | UI-04 fixed user-scoped unread | NotificationServiceTest **PASS** | NOT RUN |
| 22 | Reports | ReportsPage | ReportController | report queries | None | reports test present | NOT RUN |
| 23 | Audit | AuditLogPage + Platform access audit | AuditLogController | audit_log | None | Audit* **PASS** | NOT RUN |
| 24 | ERP Staging | ErpStagingPage | ErpStagingController | erp staging | None | erp tests **PASS** | NOT RUN |
| 25 | Import / Export | ImportExportPage | MasterDataBulkImportController | CSV/Excel | None | Excel import tests **PASS** | NOT RUN |
| 26 | Vendor Selection | VendorSelectionPage (UI-02 auto-load) | VendorSelectionController `@RequiresModule("VENDOR_SELECTION")` | VS tables | None | **49** VS unit tests in suite **PASS** | NOT RUN |
| 27 | Supplier 360 | VS page 360 tab | VS `/suppliers/{id}/360` | composite reads | None | VS suite **PASS** | NOT RUN |
| 28 | Helping AI Agent | FAB opt-in + panel (passive) | HelpingAgentController `@RequiresModule("PRODUCT_COSTING")` | no extra schema | Passive icon docs V149 | helpingagent **5** tests **PASS** | NOT RUN |
| 29 | Payment / Subscription | PlatformBillingPage | BillingController, webhook-gated entitlements | V91 billing | Approved in V134–V137 | Billing* **PASS** | NOT RUN |

### Architecture notes (no invention)

- **Logistics** is not a separate product module; freight/duty live under **Packaging & Logistics** (`PackagingLogisticsController`).
- **Material Costing** is API/engine-first; UI entry is through costing workflows, not a standalone duplicate page.
- **Purchase / Order Tracking** remain registry modules only (future) — **feature freeze**.

### Frontend consistency (inspection)

- All listed modules have dedicated routes in `App.tsx` (no `PlaceholderPage` on live routes).
- Shared MUI patterns: Paper, Tables, Alerts, loading/empty states present on major pages.
- Module nav gated by `hasModule('PRODUCT_COSTING'|'VENDOR_SELECTION')` and platform admin paths.
- Helping Agent: passive FAB; opens only on click (see `HELPING_AGENT_PASSIVE_ICON_BEHAVIOR.md`).

### Backend / security (inspection)

- Product Costing controllers: `@RequiresModule("PRODUCT_COSTING")`.
- Vendor Selection: `@RequiresModule("VENDOR_SELECTION")` + role `@PreAuthorize` on sensitive ops.
- JWT auth; Central Access tenant header `X-Customer-Id`; MFA supported on login.
- Financial engines use **BigDecimal** (unit-tested).

### Database

- Flyway **V1–V91**; 88 migration files present.
- Tenant isolation enforced in services/CAM — live cross-tenant HTTP **NOT RUN** on agent.

---

## Cross-module workflow (offline)

| Workflow step | Offline evidence | Live |
|---------------|------------------|------|
| BOM → Material → Process → Tooling → Packaging → Should-Cost → Target → RFQ → Quote → Comparison → Approval → Report | Engine/service unit tests **PASS** per step | NOT RUN end-to-end |
| Customer → entitlement → user → role → effective access → module | CentralAccess + platform unit **PASS** | NOT RUN |

---

## Automated suites this cycle

| Suite | Result |
|-------|--------|
| Backend unit (skip IT) | **PASS** 453 / 0 fail / 1 skip |
| Vitest | **PASS** 17 |
| Vite production build | **PASS** |
| Playwright E2E | **NOT RUN** (no live FE) |
| prod-smoke / helping-agent-smoke | **NOT RUN** (no live API) |
| Docker live-stack | **BLOCKED** |

---

## Defects fixed in prior increments (still in tree)

| ID | Item |
|----|------|
| UI-01 | BOM auto-load |
| UI-02 | VS supplier auto-load |
| UI-04 | Dashboard user-scoped unread notifications |
| UI-05 | lastBomNodeId |
| AUTH-01 | login awaits refreshAccess + DEFAULT tenant |
| HA-UX | Passive icon copy / docs |

**No new functional defects found in offline suite this cycle** (0 unit failures).

---

## Customer must still run (Windows)

```bat
docker compose up --build -d
curl http://localhost:8080/actuator/health
bash scripts/prod-smoke.sh
bash scripts/helping-agent-smoke.sh
cd frontend && npx playwright test
```

Plus `PRODUCTION_DRY_RUN_CHECKLIST.md` and cross-module manual workflow.

---

## Conclusion

| Dimension | Status |
|-----------|--------|
| Approved Phase-1 module development | **COMPLETE** (no missing approved modules found) |
| Offline automated regression | **PASS** |
| Live / Docker / Playwright | **BLOCKED / NOT RUN** on agent |
| Ready for customer testing phase | **YES** |

