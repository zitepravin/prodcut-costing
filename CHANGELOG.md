## 2026-08-24 — V292 complete multi-tenant demo data (V167 schema fix + V168 volume)

- **V167__complete_demo_company_data.sql** (schema alignment on unapplied demo seed):
  - Materials/machines/labor/overhead: AbstractMasterRateRecord columns (`record_id`, `customer_id`, `code`, `rate`, …)
  - BOM nodes: `customer_id`, `make_buy_flag` (no invalid `description` column)
  - Tooling: `tooling_record` correct columns (`tooling_record_id`, `bom_node_id`, `tooling_type`, …)
  - Programs: `customer_id` + `target_currency`
  - Suppliers: `country_code`
- **V168__demo_volume_extension.sql** (new):
  - AURORA materials +25, HORIZON materials +15
  - AURORA programs +5 (EV Platform, Compact SUV, CV Refresh, EPT, Premium Sedan)
  - AURORA product/BOM roots +15 (+ MAKE/BUY children)
  - HORIZON product roots +10, DEFAULT product roots +10
  - Should-cost builds +10, target costs +5
  - Suppliers AURORA +10, HORIZON +7
  - Audit events +20
- Preserves V18 DEFAULT EV-CX-2027 chain; tenants DEFAULT / AURORA / HORIZON
- Idempotent fixed UUIDs; no business-logic code changes
- **LIVE APPLY:** not executed in this environment (no MySQL runtime)

## 2026-08-23 — V292 role-wise dashboard operational KPIs

- Flyway V164: seed COMMODITY_MATERIAL_BUYER, INDUSTRIAL_ENGINEER, PRODUCT_COST_ENGINEER; dashboard count indexes
- DashboardService tenant-scoped aggregates: open RFQ, RFQ closing soon, awaiting quotations, open/overdue PO, PR, buyer workload, purchasing spend, incomplete materials, draft routings, routing exceptions, cycle-time issues, tooling investment
- Permission gates: DASHBOARD_VIEW on GET /api/v1/dashboard/kpis; RFQ_VIEW / COSTING_VIEW / SUPPORT_TICKET_READ slice zeros
- Program cost-position: COSTING_VIEW or SHOULD_COST + PRODUCT_COSTING module
- Frontend persona widgets + DashboardPage tiles/pending actions
- Tests: DashboardServiceVendorKpiTest extended; Controller/Permission/Entitlement/Tenant/Aggregation; FE persona/role/permission/entitlement/page tests


## 2026-08-21 — V280 FTA_TRADE_AGREEMENT (optional)

- V155 schema + feature catalog
- Generic agreement/origin/tariff/remedy engines
- /api/v1/fta APIs + FE page
- Golden tests; normal import still green


## 2026-08-21 — V279 Import Costing completion wave

- Multi-item shipment allocation + reconciliation
- FX via CurrencyService (explicit asOfDate)
- Duty/Tax config CRUD + approve
- FE /import-costing
- Targeted tests 38/0/0


## 2026-08-21 — Normal Import Costing (no FTA)

- V154__import_costing_normal.sql
- package com.automotive.costing.importcosting (entities, engines, service, controller)
- Feature IMPORT_COSTING
- Tests: 14/0/0 targeted
- docs/IMPORT_COSTING_MODULE.md


## 2026-08-21 — Flyway integration strategies

- docs/FLYWAY_INTEGRATION_STRATEGIES.md (A app-start, B migrate-before-traffic, C CI validate, D expand/contract)
- FlywayMigrationIntegrationTest EXPECTED_MIN_VERSION → 153
- application.yml: validate-on-migrate + clean-disabled


## 2026-08-21 — CI/CD pipeline integration

- .github/workflows/ci.yml (backend JDK21, frontend Node22, ci-verify, Docker build on main)
- .github/workflows/release-gate.yml (MySQL service + Flyway IT)
- .gitlab-ci.yml
- docs/CI_CD_PIPELINE.md


## 2026-08-21 — Universal Costing FE RBAC

- roleAccess: /universal-costing in PRODUCT_COSTING_PATHS
- roleAccess.test: costing-only can access; purchase-only cannot
- UniversalCostingPage: canMutate / canApprove write gates + read-only banner


## 2026-08-21 — FE polish + V153 industry profiles

- V153__industry_profiles_expanded.sql (17 profiles)
- UniversalCostingPage: industry profile on calculate, norm New version UI, partial SOB checkbox
- Package updated


## 2026-08-21 — Universal Costing FE shell

- frontend/src/api/universalCostingApi.ts
- frontend/src/pages/UniversalCostingPage.tsx
- Route /universal-costing + nav entry
- Backend remains V278 COMPLETE (753/0/0/1)


## 2026-08-20 — UNIVERSAL COSTING INTEGRATED (targeted)

- V152 norms + SOB + commercial/freeze columns
- CostMethodIntegrationFacade routes to real engines (no formula copy)
- NormDefinitionService, SupplierSobService, UniversalCostingService, Controller
- Lifecycle DRAFT→SUBMITTED→APPROVED→LOCKED→SUPERSEDED
- Compile BUILD SUCCESS (774 sources)
- Targeted tests: 19 run / 0 fail / 0 err / 0 skip
- Full suite not re-run


## 2026-08-18 — MASTER-V275 Sandbox non-Docker completion

- EvidenceExpiry warranty/guarantee scoped queries
- ErpPromote tenant batch + material existsByCustomerIdAndCode + BOM customerId
- RfqLine customerId on create
- ProductionHardeningValidatorTest
- SANDBOX_NON_DOCKER_COMPLETION.md

## 2026-08-18 — MASTER-V274 Sandbox hardening (no Docker)

- IdempotencyKeyFilter, MaxPageSizeWebConfig
- QuestionnairePack + EvidenceExpiry tenant-safe queries
- docker-compose.prod.example.yml
- READY FOR EXTERNAL DOCKER TESTING

## 2026-08-18 — MASTER-V273 Final hardening; NOT READY — P0 blockers

- 153-table classification complete
- V143 cost/RFQ child tables
- ProductionHardeningValidator + prod Redis rate limit
- V273_PRODUCTION_READINESS_REPORT (NOT READY)

## 2026-08-18 — MASTER-V272 ERP batch tenant + catalog seed on customer create

- V142 erp_inbound_batch customer_id
- ErpStagingService tenant-scoped
- TenantCatalogSeedService (overhead + questions from DEFAULT)
- CentralAccessService.createCustomer seeds catalog

## 2026-08-18 — MASTER-V271 Overhead pool tenant + portal RFQ ownership

- V141 overhead_pool customer_id
- OverheadPoolService tenant-scoped
- Portal grant/revoke requireOwnedRfq

## 2026-08-18 — MASTER-V270 Tooling tenant + residual fixes + evidence templates

- V140 tooling_record/calculation customer_id
- ToolingRecordService tenant asset register
- ShouldCost/ToolingCost/Rfq program-BOM tenant loads
- LOAD_TEST + DR_DRILL evidence templates
- Residual scan refresh

## 2026-08-18 — MASTER-V269 Vendor question bank tenant (V139)

- V139 vendor_question customer_id + unique keys
- QuestionnaireService list/create/answer tenant-scoped
- Dashboard pending approval = current user
- ReportService plant tenant

## 2026-08-18 — MASTER-V268 Plant/packaging/target tenant + dashboard KPIs

- V138 plant + packaging profile + target cost/variance
- PlantMasterService tenant-scoped
- Packaging + TargetCosting service enforcement
- Dashboard packaging/target/variance by customer
- Questionnaire catalog = tenant-owned (doc)

## 2026-08-18 — MASTER-V267 Dashboard tenant KPIs + V137 + questionnaire tenant

- DashboardService customer-scoped KPIs
- V137 per-tenant unique on masters + process_name
- Questionnaire response/evaluation customer_id
- Material/Machine tests CustomerContext

## 2026-08-18 — MASTER-V266 Feature continuity for tenant masters + process V136

- FEATURE_IMPACT_TENANT_MASTERS.md
- MaterialCost/ProcessCost/emissions use tenant master lookups
- V136 process_master/routing/operation customer_id
- ProcessRoutingService tenant create/load

## 2026-08-18 — MASTER-V265 Material/Machine/Labor tenant (V135) + workflow/quote

- Product decision: masters are TENANT-OWNED (classification)
- V135 + AbstractMasterRateRecord.customerId
- Material/Machine/Labor service enforcement
- WorkflowInstance + SupplierQuoteService tenant paths

## 2026-08-18 — MASTER-V264 Claim/event tenant + residual scan + MixedCost scope

- WarrantyClaim/GuaranteeEvent tenant repos
- MixedCost/AutoIntel tenant should-cost/quote loads
- RESIDUAL_FIND_BY_ID_SCAN.md + OUTBOX_PUBLISHER_OPTIONS.md
- DualTenantDataPlaneIT scaffold

## 2026-08-18 — MASTER-V263 Login Redis rate limit + V134 + email SKIP LOCKED

- LoginRateLimiter uses RateLimitStore
- V134 warranty/guarantee/policy/claim/event customer_id
- Evidence verify assertOwns on residual entities
- Email delivery FOR UPDATE SKIP LOCKED
- DataPlaneTenantIsolationContractTest

## 2026-08-18 — MASTER-V262 Evidence list tenant + outbox publisher + readiness gate

- Quality/Delivery metric repos tenant-scoped
- Evidence verify/list by customerId
- OutboxEventPublisher (logging | spring-event)
- PRODUCTION_READINESS_GATE.md
- load-tests/README.md

## 2026-08-18 — MASTER-V261 Redis rate limit + evidence/capability tenant

- RateLimitStore + InMemory + Redis implementations
- Redis auto-config only when RATE_LIMIT_STORE=redis
- SupplierEvidenceService customerId on creates
- VendorCapabilityService create + listExpiring tenant
- Scorecard lineage tenant-scoped

## 2026-08-18 — MASTER-V260 Scorecard/comparison tenant + files API + S3

- V133 customer_id scorecard template/result + comparison
- ScorecardService + SupplierComparisonService tenant-scoped
- StoredFileController
- S3ObjectStorageService + AWS SDK dependency

## 2026-08-18 — MASTER-V259 Recommendation/qualification tenant + reports + object storage

- VendorRecommendationService tenant require/create/list
- SupplierQualificationService customerId on lifecycle current
- ReportService should-cost totals by customerId
- ObjectStorageService + LocalObjectStorageService + StoredFileService
- app.storage.provider=local default

## 2026-08-18 — MASTER-V258 Vendor risk/TCO + rate limit + SC aggregates + DR

- SupplierRiskService/TCO tenant-scoped
- ShouldCost sum*ByCustomerId aggregates
- ApiRateLimitFilter (tenant+user) after CustomerContext
- FilterRegistrationConfig; outbox Micrometer counters
- DISASTER_RECOVERY_RUNBOOK.md (RPO 15m / RTO 60m)

## 2026-08-18 — MASTER-V257 PR isolation + outbox worker + V132

- PurchaseService TenantAccessGuard (requirePr/Node, list, create)
- PurchaseRequestRepository tenant finders
- DomainOutboxWorker (SKIP LOCKED, retry, DEAD)
- Flyway V132 wave-2 customer_id columns
- RATE_LIMITING_DESIGN.md
- PurchaseRequestTenantIsolationTest

## 2026-08-18 — MASTER-V256 Core service tenant isolation + PO outbox

- SupplierMaster full tenant scope
- BomNodeService requireOwned + create/what-if customerId
- RfqEvent + RfqService tenant scope
- ShouldCostBuildUp customerId + getOrThrow/generate
- PurchaseOrder APPROVE/ISSUE DomainOutbox enqueue
- SupplierMasterTenantIsolationTest

## 2026-08-18 — MASTER-V255 Data-plane multi-tenancy Wave 1

- TABLE_TENANT_CLASSIFICATION.md
- Flyway V131: customer_id on vehicle_program, bom_node, plant/material/machine/labor masters, supplier_master, should_cost_build_up, rfq_event, purchase_request, make_vs_buy, costing_run, vendor_comparison
- DEFAULT customer backfill; per-tenant unique (customer_id, program_code) / (customer_id, supplier_code)
- TenantAccessGuard; VehicleProgram service tenant-filtered; BomNode entity+repo methods
- Tests: TenantAccessGuardTest, VehicleProgramTenantIsolationTest
- Next: apply pattern to remaining services; V132 wave 2

## 2026-08-18 — MASTER-V254 Production hardening foundations

- Package rename: V253 hardening pass → official MASTER-V254
- CorrelationIdFilter, V130 outbox+stored_file, DomainOutboxService, Prometheus, Hikari guidance, k6 harness
- Architecture audit report included in project docs/artifacts
- Remaining P0: data-plane customer_id isolation, distributed rate limit, DR, load proof

## 2026-08-18 — Production scalability audit + P0 foundations (post-V253)

- Full architecture audit: multi-tenancy data plane RED; observability RED; DR RED
- CorrelationIdFilter + MDC correlationId on every request (X-Request-Id / X-Correlation-Id)
- Hikari pool env overrides (HIKARI_MAX_POOL etc.) + sizing documentation
- micrometer-registry-prometheus; Actuator exposes health,info,metrics,prometheus
- Flyway V130: domain_outbox (transactional outbox) + stored_file (object storage metadata)
- DomainOutboxEntity / DomainOutboxRepository (SKIP LOCKED claim) / DomainOutboxService.enqueue
- load-tests/k6-multi-tenant-smoke.js harness
- docs/production/PRODUCTION_HARDENING_NOTES.md
- PROJECT_STATUS / CURRENT_CONTEXT / NEXT_AGENT updated for next agent
- Remaining P0: customer_id data-plane isolation, distributed rate limit, DR, load proof

## 2026-08-17 — MASTER-V235 Wave C PR → existing RFQ

- POST /api/v1/purchase-requests/{id}/create-rfq (PURCHASE module)
- Reuses RfqService; Flyway V126 linked_rfq_id; status RFQ_LINKED

## 2026-08-17 — MASTER-V234 Quote vs should-cost + portal clarification

- GET /api/v1/rfq/events/{rfqId}/quote-comparison (buyer analysis)
- FE RFQ page comparison table
- V125 clarification + attachment metadata tables
- Portal APIs: seller ask/list clarification; list attachments; buyer respond/add attachment

## 2026-08-17 — MASTER-V233 Wave B Purchase Requisition

- V124: pr_number, plant, requester, department, buyer, required_date, priority, purpose; line material/target/cost center; supplier_id nullable
- Controlled status transitions + start-review/cancel/reopen
- Submit mandatory validation; FE plant/required date
- D1: evolve purchase_request (no duplicate RFQ)

## 2026-08-17 — MASTER-V232 Wave A module entitlement

- FE: PURCHASE/VS removed from PRODUCT_COSTING_PATHS; moduleRequiredForPath helper
- Nav: independent Purchase group for /purchase-requests
- Flyway V123: DEMO_PURCHASE_ONLY, DEMO_COSTING_ONLY, DEMO_VS_ONLY customers
- Vitest roleAccess matrix
- Backend RequiresModule unchanged (already enforced)

## 2026-08-17 — MASTER-V231 Working Purchase module (DEFAULT)

- Flyway V122: enable PURCHASE + ORDER_TRACKING entitlements for DEFAULT customer
- docs/HOW_TO_WORKING_PURCHASE_MODULE.md (gates, UI path, smoke, troubleshooting)
- Does not change approve/release policy or invent FX

## 2026-08-16 — Global module navigation hierarchy (V205)

### Architecture
- MODULES tree in routeHierarchy.ts
- Landings: /master-data, /product-costing, /sourcing, /operations
- BackToParentBar: deterministic parent (not browser history)
- Removed duplicate page breadcrumbs

### Tests
- Vitest 25 PASS; FE build PASS

## 2026-08-16 — Import/Export role UX (V204)

### Frontend
- ImportExportPage: canImport via MASTER_DATA_ADMIN; info Alert for engineers; clearer 403 text
- Deploy V203+ to remove universal Back to Dashboard

### Backend
- Unchanged: import still @PreAuthorize MASTER_DATA_ADMIN (by design)

## 2026-08-16 — UI navigation hierarchy (V203)

### Frontend
- Removed AppLayout "Back to Dashboard" on every page
- navigation/routeHierarchy.ts — parent map, breadcrumbs
- navigation/BackToParentBar.tsx — Back to immediate parent only when nested
- PageHeader optional backTo prop
- routeHierarchy.test.ts

### Tests
- Vitest 21 PASS; tsc + vite build PASS
- Playwright: NOT RUN (no live stack on agent)

### Scope
- UI/navigation only — no API, costing, security changes

## 2026-08-16 — Mandatory field UI standard (V202)

### Frontend
- components/ux/RequiredFieldLegend.tsx
- Theme: MuiFormLabel/MuiInputLabel asterisk error.main (retained)
- Required props + legends: Login, Materials, Should-Cost, Tooling, Process, Packaging, Target, VS, RFQ, ERP, Currency, Platform config

### Docs
- MANDATORY_FIELD_UI_STANDARD.md

### Backend
- No validation logic changes

## 2026-08-16 — MASTER-V201 FE role-based nav access

- auth/roleAccess.ts: path → roles/modules; canAccessPath()
- auth/RequireRole.tsx: page-level role gate helper
- AppLayout: filterItem uses canAccessPath (Audit Log + modules + platform)
- features/FEATURE_RBAC_NAV.md

## 2026-08-16 — MASTER-V200 Role-gated Audit Log nav

- AppLayout: hide `/audit` unless hasRole ADMIN or SYSTEM_ADMINISTRATOR
- Non-admin users (e.g. COSTING_ENGINEER) no longer see Audit Log in sidebar
- Page-level restriction retained if URL accessed directly

## 2026-08-15 — MASTER-V178 P3-01 hardening A/B/D

- A: Supplier qualification lifecycle must be APPROVED (fail closed)
- B: Quote must match BOM node + supplier; ACCEPTED or manualOverrideReason
- D: decided_by / released_by from SecurityContext; Flyway V98 released_by
- features/FEATURE_P3_01_HARDENING_ABD.md

## 2026-08-15 — MASTER-V177 P3-01 Purchase Request (Option D)

- Design APPROVED; §12 defaults applied
- Flyway V97 purchase_request + purchase_request_line
- Backend: com.automotive.costing.purchase.*
- API under @RequiresModule("PURCHASE")
- FE: /purchase-requests, purchaseApi.ts
- Export package read-only (no SAP MM post)
- Human-only approve/release; AI help-only

## 2026-08-15 — MASTER-V176 session package

- P3-01 DESIGN ONLY: features/FEATURE_P3_01_PURCHASE_PO_PROPOSED.md (PROPOSED REQUIREMENT)
- P3-08 specs: frontend/e2e/phase2-wave-b.spec.ts (routes + Export package UI smoke)
- No runtime code change vs V175 (Flyway still V96)
- AI help-only boundary restated; Option D blocked until design APPROVE + §12
- Package PRODUCT-COSTING-SOFTWARE-MASTER-V176.zip


## 2026-08-15 — V175 Make-vs-Buy Export package UI

- FE: `exportMakeVsBuyPackage` in makeVsBuyApi.ts
- FE: MakeVsBuyPage **Export package** button — downloads read-only JSON for CPI/SAP teams
- Does not post to SAP CO (policy unchanged)
- API_SPEC: P3-04 UI note
- Package MASTER-V175

## 2026-08-15 — V174 Phase-3 Wave B

- P3-10: FX align Make-vs-Buy + Mixed quote legs (NOT_AVAILABLE if no rate)
- P3-04: GET /api/v1/make-vs-buy/{id}/export-package


## 2026-08-15 — V173 SAP docs bundle

- docs/sap/* (field mapping, checklist, feature, design)
- No code change vs V172


## 2026-08-15 — V172 Mixed Costing source pickers

- MixedCostingPage: scenario + accepted-quote dropdowns per leg
- Completes Wave A UX with Make-vs-Buy pickers (V171)


## 2026-08-15 — V171 Wave A (P3-02 / P3-03)

- Make-vs-Buy BUY legs: VS TCO snapshot when dataComplete
- FE source pickers: should-cost scenarios + accepted quotes
- Package MASTER-V171


## 2026-08-15 — V170 Customer live gate handoff

- CUSTOMER_LIVE_GATE_HANDOFF.md (Phase-1+2)
- DEVELOPMENT_COMPLETE.md updated
- No functional code change vs V169


## 2026-08-15 — V169 Phase-2 completion docs

- API_SPEC: Mixed Costing, Make-vs-Buy, SAP CO, Lifecycle, Deep variance
- PHASE2_COMPLETION_STATUS.md
- Package MASTER-V169 (docs + V168 code)


## 2026-08-15 — V168 P2-04 Make-vs-Buy

### Backend
- Flyway V96 make_vs_buy_simulation + legs
- MakeVsBuyComparisonEngine (NOT_ELIGIBLE excluded; TCO when complete)
- compare / decide / apply-to-bom
- Engine tests written (4)

### Frontend
- /make-vs-buy page

### Package
PRODUCT-COSTING-SOFTWARE-MASTER-V168.zip


## 2026-08-15 — V167 SAP Cost Estimates FE + CPI checklist

### Frontend
- /sap-cost-estimates page (list, map, compare)
- sapCostApi.ts
- Nav under Operations

### Docs
- SAP_CPI_INTEGRATION_CHECKLIST.md

### Package
PRODUCT-COSTING-SOFTWARE-MASTER-V167.zip


## 2026-08-15 — V166 SAP CO cost estimate inbound

### Backend
- Flyway V95 sap_cost_estimate_staging + components + mapping
- POST/GET land, map-components, compare-should-cost
- SapShouldCostCompareEngine (no invented zeros)
- Tests written (4)

### Package
PRODUCT-COSTING-SOFTWARE-MASTER-V166.zip

### Not claimed
mvn full suite, Playwright, live SAP


## 2026-08-15 — V165 P2-03 Mixed Costing FE

### Frontend
- MixedCostingPage + mixedCostApi
- Route /mixed-costing, nav Product costing
- StatusChip extended (NOT_AVAILABLE, CALCULATED, FROZEN)
- vitest 17 PASS, tsc PASS

### Package
PRODUCT-COSTING-SOFTWARE-MASTER-V165.zip


## 2026-08-15 — V164 P2-03 Mixed Costing backend

### Backend
- Flyway V94 mixed_cost_version + mixed_cost_leg
- MixedCostBlendEngine (strict: missing/blocked → NOT_AVAILABLE)
- REST create/list/get/replace-legs/calculate/freeze
- MixedCostBlendEngineTest (5 cases written)

### Package
PRODUCT-COSTING-SOFTWARE-MASTER-V164.zip

### Not claimed
- mvn full suite re-run (no Maven on agent)
- FE Mixed Costing page
- Playwright

## 2026-08-14 — Customer live gate handoff (V155)

- CUSTOMER_LIVE_GATE_HANDOFF.md
- Offline unit reconfirmed: 458 PASS, 0 fail, 1 skip
- No feature changes

## 2026-08-14 — Help Agent knowledge integration (V154)

### AI_AGENT
- PRODUCT_COSTING_HELP_AGENT.md, PRODUCT_COSTING_HELP_AGENT_RULES.md, README.md
- knowledge/: 5000 Q&A (V137) + 100 extension (V153) + chunks + combined_index.json

### Notes
- Knowledge is for retrieval/explanation patterns only
- Runtime truth: read-only tools → existing services

## 2026-08-14 — Docker Compose networking (V153)

### Fix
- Frontend Dockerfile: VITE_API_BASE_URL empty (was wrong VITE_API_BASE)
- docker-compose: pcs-net network; frontend build arg VITE_API_BASE_URL=""
- vite.config.ts: proxy /api and /actuator to 127.0.0.1:8080

### Docs
- DOCKER_COMPOSE_NETWORKING_REPORT.md

## 2026-08-14 — Helping Agent offline regression + live checklist (V152)

- RuleBasedAIProviderTest: currency, AI_RECOMMENDATION, CALCULATED paths
- HELPING_AGENT_LIVE_ACCEPTANCE_CHECKLIST.md
- helping-agent-smoke classification allow-list

## 2026-08-14 — Final Help Chatbot specification alignment (V151)

### Helping Agent
- Classification: AI_RECOMMENDATION (validator + RuleBasedAIProvider)
- Tool: get_currency_context (CurrencyService + ExchangeRateService, read-only)
- Explain path: FACT / CALCULATED / AI_RECOMMENDATION by intent; no invented numbers
- FE DEFAULT_PROMPTS expanded to final-spec customer questions
- docs/HELPING_AGENT_FINAL_SPEC_COMPLIANCE.md

### Tests
- helpingagent package 15 PASS

## 2026-08-14 — Module-by-module full QA matrix (V150)

### QA
- FULL_SCALE_QA_REPORT.md — all 29 modules inspected
- Backend unit 453 PASS; Vitest 17; FE build PASS
- Live Docker/UI/API BLOCKED on agent

### Development
- No new features; no duplicate modules
- Logistics documented as part of Packaging & Logistics

## 2026-08-14 — Helping Agent passive icon behavior clarified (V149)

### Docs
- HELPING_AGENT_PASSIVE_ICON_BEHAVIOR.md — opt-in rules, flow, non-goals, test expectations

### Frontend copy
- FAB tooltip: optional; does not open by itself
- Panel static intro: silent until user asks
- Subtitle: Opens only when you ask

### Behavior (unchanged logic)
- helpOpen defaults false; no auto-open; chat API only after user question/chip

## 2026-08-14 — Helping Agent FAB opt-in clarity (V148)

- Side agent icon: tooltip “click when you want information”; no auto-open, no unsolicited messages
- Opens right drawer only on user click; answers only after user sends a question or prompt chip

## 2026-08-14 — Development complete + UI-04 (V147)

### Development gate
- DEVELOPMENT_COMPLETE.md — Phase-1 closed for testing
- No Purchase/PO feature work

### UI-04
- DashboardService: user-scoped unread notification count
- Dashboard FE label: “Unread notifications”
- DashboardServiceVendorKpiTest updated

### Regression
- Unit 453 PASS; Vitest 17; FE build PASS

## 2026-08-14 — Live UI defect fixes BOM + Vendor Selection (V146)

### Frontend
- BomPage: auto-load tree when `programId` set (dropdown / localStorage)
- BomPage: persist selected node to `pcs.lastBomNodeId`
- VendorSelectionPage: auto-load suppliers on mount
- SupplierListPanel: clearer empty state (not loaded vs no match)

### Docs
- UI_DEFECT_BACKLOG.md (UI-01..UI-05)

## 2026-08-14 — Production-hold handoff + API re-verify (V145)

### QA
- Clean DB Flyway migrate to **V91** PASS
- prod-smoke.sh PASS
- helping-agent-smoke.sh PASS

### Docs
- PRODUCTION_DRY_RUN_CHECKLIST.md → Flyway V91 + Helping Agent checks
- HANDOFF_PRODUCTION_HOLD.md, QA_MASTER_STATUS.md refreshed

## 2026-08-14 — Active customer after login (V144)

### Frontend
- AuthContext.login awaits refreshAccess so pcs.activeCustomerId is ready before callers continue
- Prefer DEFAULT customer code when assigning initial active tenant
- vendor-selection-360.spec: waitForResponse /360 2xx counts as loaded

### Why
- Intermittent 360 E2E failures when Load Supplier 360 ran before X-Customer-Id was set

## 2026-08-14 — Full Playwright regression + ACCESS-03 tenant (V143)

### QA
- Playwright: helping-agent 3, entitlement 2 (ACCESS-07 + ACCESS-03), smoke 4, VS 4, decision 2 — **16 PASS**
- ACCESS-03 requires customer with VENDOR_SELECTION disabled + membership (NOVS pattern)
- vendor-selection-360 ACME-BRG load intermittent (empty 360 / tenant header timing)

### Docs
- PLAYWRIGHT_FULL_REGRESSION_V143_REPORT.md
- module-entitlement.spec.ts env comments

## 2026-08-14 — Playwright Helping Agent UI + smoke (V142)

### QA
- Playwright: helping-agent 3 + smoke 4 = **7 PASS**
- Vitest **17 PASS**
- FAB opens Costing Help panel after login when PRODUCT_COSTING enabled

## 2026-08-14 — Host live smoke + Helping Agent live verification (V141)

### QA
- Host MariaDB + Spring Boot local: actuator UP, Flyway V91
- prod-smoke.sh PASS
- helping-agent-smoke.sh PASS (ACCESS-07 + chat FACT with sources)
- FlywayMigrationIntegrationTest PASS

### Scripts
- helping-agent-smoke.sh: tenant header, ACCESS-07 assertion, customer auto-discover

## 2026-08-14 — Unit regression 452 + Helping Agent page context (V140)

### QA
- Backend unit (exclude integration.*): **452 PASS**

### Frontend
- helpingAgentContext.ts (session context + suggested prompts by route)
- BomPage / ShouldCostPage publish context for FAB
- AppLayout uses pcs.lastProgramId + stored context

### Tooling
- scripts/helping-agent-smoke.sh
- docs/REGRESSION_V140_REPORT.md

## 2026-08-14 — Helping Agent security + Playwright + optional LLM scaffold (V139)

### Backend
- HelpingAgentToolRegistryTest, HelpingAgentAuthorizationServiceTest, HelpingAgentServiceWriteProtectionTest
- HttpAIProvider `@ConditionalOnProperty(helping-agent.llm.enabled=true)` — still delegates to rule-based
- RuleBasedAIProvider `@Primary`
- application.yml: helping-agent.llm.enabled=false by default
- Unit tests: **13 PASS**

### Frontend / E2E
- e2e/helping-agent.spec.ts (API unauth 401; authenticated classified response; UI FAB)

### Database
- None

## 2026-08-14 — Customer Helping AI Agent Product Costing (V138)

### Backend
- Package `helpingagent`: controller, service, context, authorization, tool registry, rule-based AI provider, audit
- Read-only tools map to existing BOM/material/process/tooling/packaging/should-cost/target/comparison/quote/version/report/workflow services
- `POST /api/v1/helping-agent/chat` + `@RequiresModule("PRODUCT_COSTING")`
- Unit tests: RuleBasedAIProviderTest (4), HelpingAgentResponseValidatorTest (3) — 7 PASS

### Frontend
- `components/helpingagent/HelpingAgentPanel.tsx`
- `api/helpingAgentApi.ts`
- AppLayout FAB visible when PRODUCT_COSTING module enabled

### Database
- None (preferred first implementation)

### Risk
- Rule-based answers only until optional LLM provider configured
- Context identifiers must be supplied by UI; free-text alone cannot invent tenant scope

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

## 2026-08-18 — V237 compile + Docker Dockerfile fix

### RfqDtos.java: nested QuoteShouldCostComparison* records (was invalid public top-level)
### backend/Dockerfile: maven:3.9.9-eclipse-temurin-21 multi-stage (no apt maven)
### Offline mvn -DskipTests package SUCCESS; targeted unit tests PASS
### Docker daemon still unavailable on packaging agent — host must build images

## 2026-08-18 — Purchase module related work COMPLETE (gate)

### PURCHASE_MODULE_COMPLETION_GATE.md = PASS (Waves A–C + P3-01)
### Wave D full PO still NOT STARTED (explicit auth required)
### Continuity: PROJECT_STATUS / CURRENT_CONTEXT / NEXT_AGENT updated

## 2026-08-18 — Wave D Purchase Order (V238)

### V127 purchase_order + purchase_order_line
### PurchaseOrderService/Controller @RequiresModule(PURCHASE)
### FE /purchase-orders + Purchase nav hierarchy
### PurchaseOrderStatusTest 7 PASS; compile SUCCESS

## 2026-08-18 — V239 PO lifecycle offline smoke

### PurchaseOrderLifecycleTest 3 PASS + StatusTest 7 PASS
### scripts/purchase-order-smoke.sh + v238-host-po-gate.sh
### Live V127 migrate BLOCKED on agent (no Docker)

## 2026-08-18 — Wave E notification events (V240)

### NotificationType RFQ/workflow/SYSTEM_ALERT
### NotificationEventService + RfqLifecycleEvent + listener
### RFQ/Quote publish hooks; tests 16 PASS

## 2026-08-18 — Wave F email queue (V241)

### V128 templates/delivery/preference
### Queue idempotency + retry; renderer safe placeholders
### Tests 15 PASS

## 2026-08-18 — Wave G security (V242)

### PurchaseModuleGateTest + PurchaseOrderSecurityTest + Support RO PO paths
### Security suite 24 PASS

## 2026-08-18 — Wave H host gate packaging (V243)

### scripts/wave-h-host-gate.sh
### Docker/Playwright BLOCKED on agent — HOST_REQUIRED

## 2026-08-18 — PR → PO (V244)

### POST purchase-requests/{id}/create-purchase-order
### FE Create PO from PR; tests 10 PASS

## 2026-08-18 — PO light close (V245)

### PARTIALLY_RECEIVED + CLOSED; close API + FE; tests 16 PASS

## 2026-08-18 — Purchase non-goals locked

### PURCHASE_NON_GOALS.md: no GR/IR/SAP invent

## 2026-08-18 — V247 PR externalPoReference → PO copy

### No SAP call; tests 8 PASS

## 2026-08-18 — P2P cycle feature (V248)

### FEATURE_P2P_CYCLE.md + p2p-cycle-smoke.sh + PurchaseOrderP2pCycleTest
### 18 PASS

## 2026-08-18 — Module independence Purchase (V249)

### Standalone PR + VS-optional supplier gate + V129

## 2026-08-18 — Purchase phase complete (V250)

### FE standalone PR; STORE deferred future
### PURCHASE_MODULE_COMPLETE.md

## 2026-08-18 — Purchase COMPLETE (V251)

### 47/47 purchase unit tests PASS; phase closed

## 2026-08-18 — Docker build readiness (V252)

### FE TS unused locals fixed; BE package + FE build PASS

## 2026-08-18 — Docker cache layers (V253)

### BuildKit mounts; src/main-only BE; selective FE COPY

---

## 2026-08-18 — MASTER-V276

**Feature:** Static P0 production overlay + test suite closure packaging  
**Change:** Re-applied v275-static-p0-src production services/repos and HighRiskFindByIdTenantIsolationTest; packaged governance + V275/V276 reports  
**Database:** None  
**API:** None  
**Tests:** Suite still NOT GREEN (5F/11E); Category A/B only  
**Risk:** Low — production security retained; no authorization weakened  
**Notes:** Electronics blocked until full suite green. No MASTER claim of production readiness.

## V286 — Final Production Hardening (2026-08-21)

### Closed (code)
- Fine-grained @PreAuthorize on FtaController, ImportCostingController, UniversalCostingController
  (VIEW / CREATE / SUBMIT / APPROVE / LOCK / DELETE / CONFIG)
- Feature entitlement remains necessary but is no longer sufficient for mutate actions
- GlobalExceptionHandler maps ACCESS-TENANT-* and ACCESS-FEAT-* to HTTP 403
- FineGrainedRbacAnnotationTest (8 tests)

### Not weakened
- TenantAccessGuard, module entitlement, feature entitlement, freeze, audit, historical FX

### Environment limits (not re-executed here)
- Live HTTP role matrix, live MySQL golden, host backup/restore, k6

## V286.1 — Commercial License, Limits & Offline Entitlement (2026-08-21)

### Added
- Flyway V157: customer_license, customer_role_limit, license_history, offline_license_package
- CommercialLicenseService (assertLicenseActive, assertTotalUserLimit, assertRoleUserLimit, offline import)
- CommercialLicenseController (Platform Owner manage + Customer Admin usage)
- CustomerUserAdminService hooks: create/update roles enforce license + quotas
- CentralAccessService delegates: assertLicenseActive, assertTotalUserLimit, assertRoleUserLimit
- CommercialLicenseServiceTest (10 tests)

### Preserved
- Existing RBAC, TenantAccessGuard, module/feature entitlement, audit, freeze
- No customer business data deleted on license expiry/suspension

## V286.2 — Commercial entitlement gap closure (2026-08-21)

- PlantMasterService: plant_site_limit enforcement → PLANT_LIMIT_EXCEEDED
- RequiresModuleAspect: mutating methods call assertLicenseActiveIfLicensed before module check
- Frontend: Customer Admin License & Usage page (view-only)
- Offline package: expired validTo rejected
- CommercialLicenseServiceTest: 15 tests including concurrent role-limit denial
