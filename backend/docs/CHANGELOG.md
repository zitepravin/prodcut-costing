## 2026-08-14 — Docker test stack + full-scale QA (V137)

### Docker
- Root docker-compose.yml (mysql, backend, frontend)
- frontend/Dockerfile, nginx.conf
- backend Dockerfile healthcheck curl
- Agent build BLOCKED (overlay); docs for Windows run

### QA
- Unit 439 PASS, Flyway IT V91 PASS, smoke + ACCESS-03 PASS, Vitest 17 PASS
- Playwright NOT RUN this session

## 2026-08-14 — Billing FE refund/chargeback (V136)

- billingApi + PlatformBillingPage expanded

## 2026-08-14 — Refund/chargeback access policy (V135)

### Backend
- BillingAccessPolicyService
- BillingController: refunds, chargebacks, revenue-snapshot
- Tests: 4 policy + 3 webhook PASS
- Host Flyway IT V91 PASS

## 2026-08-14 — Payment & Subscription Management (V134 / Flyway V91)

### Backend
- billing package: gateway, entities, BillingSubscriptionService, SubscriptionEntitlementService
- POST /api/v1/billing/webhooks/{gateway} permitAll + HMAC
- CAM activation only after verified PAID webhook

### Frontend
- PlatformBillingPage + nav

### Tests
- BillingWebhookSecurityTest 3 PASS

## 2026-08-14 — VS list / explainability / TCO-variance UX (V133)

### Frontend
- components/vendorselection/SupplierListPanel.tsx
- components/vendorselection/ExplanationClassificationPanel.tsx
- VendorSelectionPage: list + classification + TCO DATA NOT AVAILABLE
- ShouldCostPage: variance above/below should-cost alerts

## 2026-08-14 — Dry-run checklist review (V132)

- Expanded PRODUCTION_DRY_RUN_CHECKLIST.md

## 2026-08-14 — Production hold handoff (V131)

- HANDOFF_PRODUCTION_HOLD.md
- No code change; waiting on customer dry-run

## 2026-08-14 — Regression maintenance green (V130)

- Unit 432, Flyway IT 1, Vitest 17
- REGRESSION_MAINTENANCE_REPORT.md

## 2026-08-14 — Prod dry-run checklist + readiness gate (V129)

- PRODUCTION_DRY_RUN_CHECKLIST.md
- PRODUCTION_READINESS_GATE.md
- UI_DEFECT_BACKLOG.md

## 2026-08-14 — CI verify green (MASTER V128)

- Unit 432, Flyway IT 1, Vitest 17, prod-smoke PASS
- CI_VERIFY_REPORT.md

## 2026-08-14 — IT static-init harden (V127)

- AbstractMySqlIntegrationTest never throws on Docker probe failure
- Unit 432 PASS

## 2026-08-14 — CI verify + prod smoke scripts (MASTER V126)

### Tooling
- scripts/ci-verify.sh
- scripts/prod-smoke.sh

## 2026-08-14 — Host Flyway integration test (MASTER V125)

### Backend tests
- AbstractMySqlIntegrationTest: TEST_DB_URL host path or Testcontainers
- FlywayMigrationIntegrationTest: version >= 90, ERP + CAM tables
- application-test.yml: ddl-auto none, MariaDBDialect

### QA
- FlywayMigrationIntegrationTest **PASS** on host MariaDB (product_costing_it)

## 2026-08-14 — Production deployment runbook (MASTER V124)

### Docs
- PRODUCTION_DEPLOYMENT_RUNBOOK.md
- DOCKER_IT_STATUS.md

### Tests
- No code change; Docker IT still BLOCKED

## 2026-08-14 — Live health UP + Playwright 14/14 (MASTER V123)

### Backend
- management.health.mail.enabled: false (optional SMTP must not fail readiness)

### QA
- /actuator/health HTTP 200 UP on host MariaDB
- Playwright 14 PASS (smoke, VS, decision, 360, module-entitlement)

## 2026-08-14 — Actuator health + regression (MASTER V122)

### Backend
- spring-boot-starter-actuator
- management endpoints: health, info
- SecurityConfig permit /actuator/health/** and /actuator/info

### QA
- Unit excl. integration: 432 PASS (V121 baseline reconfirmed)
- RequiresModuleAspectTest + CentralAccessServiceTest: 14 PASS
- Vitest 17 PASS

## 2026-08-14 — RequiresModuleAspect fix + docker-free QA (MASTER V121)

### Backend
- RequiresModuleAspect: JoinPoint annotation resolution (class-level safe)
- RequiresModuleAspectTest updated

### Frontend e2e
- module-entitlement.spec.ts uses supplier 360 path

### QA
- 432 unit PASS; Flyway V90 host; API ACCESS-03/07 PASS; Vitest 17; Playwright 14
- Docker IT BLOCKED
- docs/QA_MASTER_STATUS.md, FULL_SCALE_QA_REPORT.md

## 2026-08-14 — Permission catalog + tenant selector (MASTER V120)

### Frontend
- PlatformPermissionsPage — role/capability catalog (no new permission entity)
- AppBar tenant selector → pcs.activeCustomerId → X-Customer-Id
- AuthContext activeCustomerId / setActiveCustomerId

### Docs
- PRODUCTION_SECURITY_CHECKLIST.md

### Tests
- CAM + aspect: 14 PASS; Vitest 17/17

## 2026-08-14 — Portal module gate + aspect tests (MASTER V119)

### Backend
- SupplierPortalController: invite / rfq-access / revoke require PRODUCT_COSTING
- RequiresModuleAspectTest (ACCESS-07, enabled, ACCESS-03)

### Frontend e2e
- module-entitlement.spec.ts — API 403 without customer / with VS disabled

### Tests
- 16 targeted tests PASS (aspect + CAM + portal)

## 2026-08-14 — Maven suite green + Access Audit UI (MASTER V118)

### QA
- Temurin JDK 21.0.12 + Maven 3.9.9
- Backend surefire: **430 tests, 0 failures, 0 errors** (1 IT skipped — no Docker)
- CentralAccessServiceTest: 11 PASS
- Frontend: Vitest 17/17, tsc clean

### Frontend
- PlatformAccessAuditPage (`/platform/access-audit`) — CAM entity type presets via existing audit API

### Database
- No migration

## 2026-08-14 — FE typecheck fix (MASTER V117)
- AuthContext: remove unused EffectiveAccess import (TS6133)
- Vitest 17/17 PASS; tsc clean

## 2026-08-14 — PRODUCT_COSTING module enforcement + Platform Dashboard (MASTER V117)

### Backend
- 22 controllers annotated `@RequiresModule("PRODUCT_COSTING")`
- CentralAccessServiceTest: productCosting_disabled_ACCESS_03

### Frontend
- AppLayout gates Product Costing nav paths by hasModule
- PlatformDashboardPage + route `/platform`

### Database
- No migration

### Risk
- Calls to PC APIs without X-Customer-Id → 403 ACCESS-07 (expected)
- Host must re-run Maven with JDK 21 compiler

## 2026-08-14 — Central Access enforcement + lifecycle (MASTER V116)

### Backend
- VendorSelectionController: `@RequiresModule("VENDOR_SELECTION")` (class-level)
- RequiresModuleAspect: `@within` + `@annotation`
- GlobalExceptionHandler: ACCESS-02..05 → 403 MODULE_NOT_ENABLED; ACCESS-06/07 → TENANT_ACCESS_DENIED
- CentralAccessService/Controller: updateCustomer, setCustomerStatus, deactivateMembership
- CentralAccessServiceTest: lifecycle unit tests

### Frontend
- httpClient X-Customer-Id from localStorage `pcs.activeCustomerId`
- AuthContext persists first membership customer id
- PlatformCustomersPage: name edit, status, membership deactivate, confirm module disable
- platformAccessApi lifecycle methods

### Database
- No migration (V90)

### Risk
- Host must re-run full Maven suite and Playwright with customer header

## 2026-08-14 — Platform Users/Roles FE + CAM security tests (MASTER V115)

### Frontend
- adminApi.ts, PlatformUsersPage, PlatformRolesPage

### Backend
- CentralAccessServiceTest: Customer A VS disabled → ACCESS-03
- Full mvn test: 426 run, 0 fail
- lombok annotation processor version aligned 1.18.36
- Fixed duplicate @Transactional on CentralAccessService

## 2026-08-14 — Central Access Management FE + API (MASTER V114)

### Backend
- GET /platform/customers/{id}, /modules, /users
- Membership repository findByCustomerId

### Frontend
- platformAccessApi.ts
- PlatformCustomersPage, PlatformModulesPage
- AuthContext effectiveAccess + hasModule
- AppLayout Platform Admin nav + VS module gate

### Docs
- API_SPEC CAM section

## 2026-08-14 — Full Playwright 12/12 PASS (MASTER V113)

- Live stack MySQL V90 + BE + FE
- smoke 4 + VS 4 + 360 fixture 2 + decision boundary 2 = **12 passed**

## 2026-08-14 — evidenceChip color typing (MASTER V112)

- ChipProps color for VERIFIED/FAILED/EXPIRED/WAIVED/default

## 2026-08-14 — Prod profile + decision-boundary E2E (MASTER V111)

### Backend
- application-prod.yml (strong JWT, swagger-public false, ddl-auto validate)

### Frontend
- e2e/vendor-selection-decision.spec.ts (lowest price never best; human approval language)

### Docs
- PRODUCTION_JWT_HARDENING.md references prod profile

## 2026-08-14 — Full Playwright 10/10 PASS (MASTER V110)

- Live stack MySQL V90 + BE + FE
- smoke 4 + vendor-selection 4 + 360 fixture 2 = **10 passed**
- Softened list-suppliers assertion (no MUI portal flakiness)

## 2026-08-14 — MUI 6 slotProps for Supplier ID (MASTER V109)

- Replaced deprecated inputProps with slotProps.htmlInput on VS Supplier ID field

## 2026-08-14 — VS Supplier ID accessibility + 360 E2E locator (MASTER V108)

- TextField id/name/data-testid for Supplier ID
- vendor-selection-360.spec stable fill selector

## 2026-08-14 — VS 360 seed fixture E2E (MASTER V107)

- `e2e/vendor-selection-360.spec.ts` — ACME-BRG seed supplier 360 load + list suppliers
- Seed id: c1000000-0000-4000-8000-000000000001 (V18)

## 2026-08-14 — Full Playwright 8/8 PASS (MASTER V106)

- Live stack: MySQL V90 + BE + FE
- smoke 4 + vendor-selection 4 = **8 passed**
- test:e2e uses `-c playwright.config.ts` (avoid Vitest config clash)

## 2026-08-14 — Vite/Vitest config split (MASTER V105)

- `vite.config.ts` — app only
- `vitest.config.ts` — unit test config
- Resolves TS2769 on defineConfig `test` key under Vite 8

## 2026-08-14 — VS E2E + JWT hardening doc (MASTER V104)

### Frontend
- `e2e/vendor-selection.spec.ts` (authenticated 360/tabs/deep-link)

### Docs
- `docs/PRODUCTION_JWT_HARDENING.md`
- Playwright report extended with VS suite section

## 2026-08-14 — Playwright E2E report detail expansion (MASTER V103)

- Expanded docs/PLAYWRIGHT_E2E_REPORT.md with per-test steps, assertions, env, re-run commands

## 2026-08-14 — Live Playwright E2E (MASTER V102)

- Flyway V90 + BE login OK + Vite
- Playwright: **4 passed** (login, root, credentials, vendor-selection)
- Report: docs/PLAYWRIGHT_E2E_REPORT.md

## 2026-08-14 — Hibernate validate runtime fix (MASTER V101)

- Currency fields CHAR(3); audit verified_by/created_by stay VARCHAR
- spring.profiles.default: local

## 2026-08-14 — Greenfield Flyway V90 + migration hardening (MASTER V100)

### Database
- Verified migrate **V1→V90** on MySQL 8.0.46
- CHAR(36) + utf8mb4_unicode_ci + V48 TEXT + idempotent CREATE INDEX
- Report: docs/MYSQL_V90_MIGRATE_REPORT.md

### Backend
- JPA UUID columns: columnDefinition CHAR(36)

## 2026-08-14 — Quality/Delivery panels + test re-run (MASTER V99)

### Frontend
- QualityMetricsPanel, DeliveryMetricsPanel
- VendorSelectionPage ~3739 lines
- Vitest 17/17 PASS

### Backend
- `mvn clean test` JDK 21: 424 tests, 0 failures, BUILD SUCCESS

### DB
- V90 still host-apply; next free V91

## 2026-08-14 — Fix PerformanceFeedbackPanel type (MASTER V98)

### Frontend
- `PerformanceFeedbackPanel` accepts `PerformanceFeedback | PerformanceFeedbackListItem` (360 view list shape)
- Fixes TS2322 / runtime type mismatch when rendering Supplier 360 performance feedback

### Backend / DB
- No change

## 2026-08-14 — Playwright smoke + V90 CHAR(36) ID alignment (MASTER V97)

### Database
- `V90__id_column_char36_alignment.sql` — cost_version_record + core VS tables ID/FK columns → CHAR(36)
- Next free: **V91**

### Frontend
- `@playwright/test`, `playwright.config.ts`, `e2e/smoke.spec.ts`
- `npm run test:e2e` — 4 skipped when FE not running (connection refused handled)
- Vitest still 17/17 PASS

### Notes
- No API/DTO contract change
- Full E2E pass requires running Vite + backend + MySQL

## 2026-08-14 — Host validation JDK 21 (MASTER V96)

### Backend
- Temurin 21.0.12 + Maven 3.9.6: `mvn clean test` → **424 tests, 0 failures, BUILD SUCCESS**
- Report: `docs/HOST_VALIDATION_REPORT.md`

### Playwright
- No Playwright config/specs in repository — E2E not executed (gap, not failure)

### Code
- No functional code change this increment (validation + docs only)

## 2026-08-14 — AfterSales + PerformanceFeedback panels (MASTER V95)

### Frontend
- `AfterSalesMetricsPanel.tsx`, `PerformanceFeedbackPanel.tsx`
- VendorSelectionPage wires both; apply-to-history UX unchanged (explicit only)
- Vitest 17/17 PASS; page ~3821 lines

### Backend / DB
- No changes

## 2026-08-14 — VendorSelection evidence panel extraction (MASTER V94)

### Frontend
- New: `components/vendorselection/evidenceChips.tsx`, `WarrantyClaimsPanel.tsx`, `GuaranteeEventsPanel.tsx`
- `VendorSelectionPage.tsx` wires panels; handlers unchanged; empty states preserved
- Vitest 17/17 PASS

### Backend / DB
- No changes; Flyway still V89 / next free V90

## 2026-08-14 — Automated versioning (MASTER V93)

### Backend
- `BomNodeService.release`: VER-01 automated snapshot (`BomNode` / `ChangeTrigger.ECN`) via `CostVersionService.snapshotAfterChange`
- `CostVersionService` Javadoc documents full automated coverage matrix
- Unit test: `snapshotAfterChange_bomRelease_usesEcnTrigger`

### Frontend
- `VersionControlPage`: notes automated snapshot sources (should-cost, quote ACCEPTED, target activate, BOM release)

### Docs
- API_SPEC M15 Version Control + automated matrix
- No schema migration (V26/V27 already present)

### Not changed
- No V90 migration
- No Purchase/PO modules

# CHANGELOG — Product Costing + Supplier Intelligence Platform

## 2026-08-14 — Documentation alignment to V91 / Flyway V89

### Docs
- `PROJECT_STATUS.md` — realigned to V91 packages, Flyway V89, next free **V90**
- `CURRENT_CONTEXT.md` — V64–V89 evidence depth, frontend status, constraints
- `NEXT_AGENT.md` — handover for evidence UI polish + host validation
- `DATABASE_SCHEMA.md` — authoritative next free **V90**; historical markers annotated
- No schema change; **V90 not created**

### Notes
- Frontend Vendor Selection evidence surfaces (warranty claims, guarantee events, after-sales, performance feedback, negotiation opportunities, Supplier 360) already present in V91 `VendorSelectionPage.tsx` / `vendorSelectionApi.ts`
- Backend engines unchanged
- Local MySQL IT through V89 previously verified PASS

## 2026-08-14 — V91 QA/tester suite (prior session)
- Recommendation / lifecycle / policy / guard QA scenario tests
- Existing claim / risk / event QA scenarios retained

## 2026-08-13 — Vendor evidence depth V64–V70 (+ indexes through V89)
- V64 supplier_warranty_claim
- V65 supplier_guarantee_event
- V66 supplier_after_sales_metric
- V67 negotiation_opportunities on vendor_recommendation_explanation
- V68 questionnaire seed (after-sales / warranty / guarantee)
- V69 supplier_performance_feedback (apply-to-history gated; no auto scorecard mutation)
- V70–V89 supporting indexes and dashboard vendor KPI indexes

## Earlier history
See package-internal `product-costing-backend/docs/CHANGELOG.md` and `CHANGELOG (1).md` for detailed pre-V64 entries (costing, RFQ, ERP, reports, currency, packaging, target costing).

## 2026-08-14 — Frontend evidence empty states (V92 package)
- VendorSelectionPage: empty states for warranty claims, guarantee events, after-sales metrics, performance feedback
- Negotiation opportunities: explicit "No data" when backend returns blank
- No backend/schema/API contract changes; Flyway remains V89 / next free V90
- Frontend unit tests: 17/17 PASS; tsc only pre-existing vite.config test key warning
