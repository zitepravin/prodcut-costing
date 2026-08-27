# API_SPEC.md — REST surface (as implemented)

Base path: `/api/v1`  
Auth: `Authorization: Bearer <access_token>` (except login/refresh)  
Error shape: `ApiErrorResponse` (code, message, details, timestamp, path)

## Auth & Platform

| Method | Path | Roles | Notes |
|--------|------|-------|-------|
| POST | /auth/login | public | returns access + refresh |
| POST | /auth/refresh | public | |
| POST | /auth/logout | authenticated | |
| GET/POST | /admin/** | ADMIN | user / role management |
| MFA endpoints | /mfa/** | authenticated | TOTP |

## Master Data (`/api/v1/master/...`)

- Materials: CRUD-minus-delete, version, activate/deactivate, search/paged, bulk CSV import
- Machines, Labor, Overhead Pools, Vehicle Programs — same patterns
- Controllers: MaterialMasterController, MachineMasterController, LaborMasterController, OverheadPoolController, VehicleProgramController, MasterDataBulkImportController

## BOM (`/api/v1/bom`)

- GET /{programId}/tree
- GET /nodes/{nodeId}
- POST /{nodeId}/children , POST /roots
- PUT /nodes/{nodeId}
- PUT /nodes/{nodeId}/release
- POST /nodes/{nodeId}/manual-override , DELETE .../manual-override
- POST /{nodeId}/rollup , POST /{programId}/rollup-program
- POST /{nodeId}/what-if-branch
- GET /{nodeId}/flatten

## Material Costing (`/api/v1/costing/material`)

- POST /calculate
- POST /compare
- GET /price-trend/{materialCode}
- POST /high-cost-check
- POST /commodity-index
- GET /commodity-index/{indexCode}/history

## Process Costing (`/api/v1/costing/process` and routing)

- ProcessCostController, ProcessRoutingController — calculate, CRUD routing/operations

## Tooling (`/api/v1/tooling` and costing)

- ToolingRecordController, ToolingCostController — asset CRUD, retire, calculate, compare

## Should-Cost (`/api/v1/should-cost` approx.)

- ShouldCostController, BenchmarkController — build-up, parametric/SG&A benchmarks, approval status linkage

## Workflow (`/api/v1/workflow`)

- POST /submit
- POST /steps/{stepInstanceId}/decision
- GET /inbox , GET /history
- Workflow definitions CRUD under /workflow/definitions

## Notifications (`/api/v1/notifications`)

- List, mark-read, etc. (NotificationController)

> Exact request/response DTOs live in the corresponding `*Dtos.java` classes. Prefer reading those + OpenAPI at `/swagger-ui.html` once the app is running.


## Currency Management (`/api/v1/currency`)

| Method | Path | Roles | Notes |
|--------|------|-------|-------|
| GET | /currency | authenticated | List currencies |
| GET | /currency/{code} | authenticated | Single currency |
| POST | /currency/rates | MASTER_DATA_ADMIN, COSTING_ENGINEER, ADMIN | Publish append-only rate |
| GET | /currency/rates/{from}/{to} | authenticated | Pair history |
| GET | /currency/rates?since= | authenticated | Recent rates |
| POST | /currency/convert | authenticated | Convert amount (DIRECT / INVERSE / VIA_USD) |


## Materials version history

| Method | Path | Roles | Notes |
|--------|------|-------|-------|
| GET | /master/materials/by-code/{code}/versions | authenticated | MDM-02 history, newest first |

## Workflow inbox StepResponse (extended)

Includes `workflowInstanceId`, `entityType`, `entityId` in addition to step fields.

## Multi-currency roll-up (runtime behaviour)

BOM roll-up (`POST /bom/{nodeId}/rollup`, program roll-up) and Should-Cost generate convert
calculated component costs from `app.costing.base-currency` into the vehicle program's
`targetCurrency` via Currency Management convert semantics. No new public endpoints.
Manual BOM cost overrides are not FX-converted.

## Packaging & Logistics (`/api/v1/packaging-logistics`)

| Method | Path | Roles | Notes |
|--------|------|-------|-------|
| POST | /profiles | COSTING_ENGINEER, PURCHASING_BUYER, ADMIN | Create profile for BOM node |
| PUT | /profiles/{id} | COSTING_ENGINEER, PURCHASING_BUYER, ADMIN | Update profile |
| GET | /profiles/{id} | authenticated | Get profile |
| GET | /profiles?bomNodeId= | authenticated | List by BOM node |
| POST | /profiles/{id}/deactivate | COSTING_ENGINEER, ADMIN | Soft deactivate |
| POST | /calculate | COSTING_ENGINEER, PURCHASING_BUYER, ADMIN | Calculate packaging+freight+duty |
| GET | /calculations/latest?bomNodeId= | authenticated | Latest calc for node |
| GET | /calculations?bomNodeId= | authenticated | History |
| POST | /compare | authenticated | Compare up to 5 profiles |

## Target Costing (`/api/v1/target-costing`)

| Method | Path | Roles | Notes |
|--------|------|-------|-------|
| POST | /targets | COSTING_ENGINEER, COSTING_MANAGER, ADMIN | Create DRAFT target |
| PUT | /targets/{id} | COSTING_ENGINEER, COSTING_MANAGER, ADMIN | Update |
| POST | /targets/{id}/activate | COSTING_ENGINEER, COSTING_MANAGER, ADMIN | Activate; supersede prior ACTIVE |
| POST | /targets/{id}/mark-submitted | COSTING_ENGINEER, COSTING_MANAGER, ADMIN | After workflow submit |
| GET | /targets/{id} | authenticated | Get |
| GET | /targets?bomNodeId= or programId= | authenticated | List |
| POST | /variance | COSTING_ENGINEER, COSTING_MANAGER, PURCHASING_BUYER, ADMIN | Persist variance snapshot |
| GET | /variance?bomNodeId= | authenticated | History |
| POST | /variance/exceeding | authenticated | KPI: over-threshold list |

Workflow: use existing POST /workflow/submit with entityType=`TargetCostRecord`.

## Dashboard (`/api/v1/dashboard`)

| Method | Path | Roles | Notes |
|--------|------|-------|-------|
| GET | /kpis | authenticated | Operational counts |


## Reports (`/api/v1/reports`)

Authenticated operational analytics. Monetary fields are BigDecimal (JSON numbers).

| Method | Path | Roles | Purpose |
|--------|------|-------|---------|
| GET | /operational?programId= | authenticated | Composite report; optional program scope for BOM/SC/variance |
| GET | /operational/export?programId= | authenticated | CSV export (`section,metric,value`) |
| GET | /master-data-health | authenticated | Total vs ACTIVE counts (system-wide) |
| GET | /erp-staging | MASTER_DATA_ADMIN, ADMIN, SYSTEM_ADMINISTRATOR | Batch counts by status |
| GET | /should-cost-elements?programId= | authenticated | SC element sums (optional program) |
| GET | /target-variance?programId= | authenticated | Variance KPIs (optional program) |


## M09/M10 RFQ & Supplier Costing (`/api/v1/rfq`)

### Suppliers
- `POST /suppliers` — create
- `GET /suppliers` — list ACTIVE
- `GET /suppliers/{id}`
- `POST /suppliers/{id}/status?status=`

### RFQ events
- `POST /events` — create DRAFT
- `GET /events?programId=`
- `GET /events/{rfqId}` — detail + lines
- `POST /events/{rfqId}/open|close|cancel`
- `POST /events/{rfqId}/lines` — add BOM line

### Quotes
- `POST /events/{rfqId}/quotes` — submit (RFQ must be OPEN)
- `GET /events/{rfqId}/quotes` — list (SEC-04 wall when configured)
- `POST /quotes/{quoteId}/status` — ACCEPTED supersedes prior ACCEPTED on same BOM node; sets RFQ AWARDED
- `GET /quotes/accepted?bomNodeId=` — M10 cost source

BUY BOM leaves resolve unit cost from latest ACCEPTED quote (else 0).

## Cost Comparison Workspace
- `GET /api/v1/cost-comparison/by-bom-node/{bomNodeId}` — should-cost scenarios, quotes, tooling, targets

## Master Import/Export expansion
- `POST /api/v1/master/bulk-import/materials|machines|labor` (multipart CSV or Excel .xlsx)
- `GET /api/v1/master/export/materials|machines|labor` (text/csv)

## ERP Staging (`/api/v1/erp/staging`) — V17 + V19 promote + CSV + lifecycle
- `POST /batches` — JSON ingest parts / BOM lines / material prices
- `POST /batches/csv/material-prices` — multipart CSV or Excel .xlsx (partial success; dispatch by filename)
- `POST /batches/csv/parts` — multipart CSV or Excel .xlsx
- `POST /batches/csv/bom` — multipart CSV or Excel .xlsx
- `POST /batches/{batchId}/validate` — mark rows VALID/INVALID; batch READY or FAILED
- `POST /batches/{batchId}/promote` — promote READY batch to live masters
  - Material prices → MaterialMaster versions when code exists
  - Parts → PROMOTED acknowledgement
  - BOM → BomNode tree when `program_code` resolves
- `POST /batches/{batchId}/cancel` — cancel non-PROMOTED batch (ERP-04/05)
- `POST /batches/{batchId}/reopen` — CANCELLED/FAILED → RECEIVED; reset rows PENDING (ERP-06)
- `GET /batches` — list
- `GET /batches/{batchId}` — detail
- Roles for mutating: MASTER_DATA_ADMIN, ADMIN, SYSTEM_ADMINISTRATOR
- Audit on ingest, validate, promote, cancel, reopen

CSV headers:
- material prices: `materialCode,unitPrice,currencyCode,effectiveFrom,effectiveTo,plantCode,uom,supplierCode`
- parts: `externalPartId,partNumber,revision,description,netWeightKg,uom,materialCode,makeBuyCode,plantCode,programCode`
- BOM: `parentPartNumber,childPartNumber,quantityPer,parentRevision,childRevision,uom,lineNumber,plantCode,programCode`


## Audit Log (`/api/v1/audit`) — compliance read API
| Method | Path | Roles | Notes |
|--------|------|-------|-------|
| GET | /audit | ADMIN, SYSTEM_ADMINISTRATOR | Paged query. Filters: `entityType`+`entityId`, or `userId` (+ optional `from`/`to`), or `entityType` (+ optional `from`/`to`). Default sort timestamp DESC, size 50. At least one filter required. |

Response page of `AuditLogResponse` (auditId, entityType, entityId, action, oldValue, newValue, userId, timestamp, ipAddress). Write path remains exclusive to `AuditService` (no UPDATE/DELETE).

## Vendor Selection — evidence depth (V64–V69)

Base path: `/api/v1/vendor-selection`

| Method | Path | Roles | Notes |
|--------|------|-------|-------|
| POST | /warranty-claims | BUYER, SOURCING_LEAD, MANAGER, ADMIN | Create claim; evidence PENDING |
| POST | /warranty-claims/{id}/verify | SOURCING_LEAD, MANAGER, ADMIN | Verify/fail evidence |
| GET | /suppliers/{id}/warranty-claims | authenticated | Claim summary aggregates |
| POST | /guarantee-events | BUYER, SOURCING_LEAD, MANAGER, ADMIN | Failure/breach event |
| POST | /guarantee-events/{id}/verify | SOURCING_LEAD, MANAGER, ADMIN | Verify event evidence |
| POST | /after-sales-metrics | BUYER, SOURCING_LEAD, MANAGER, ADMIN | Period after-sales KPI |
| POST | /after-sales-metrics/{id}/verify | SOURCING_LEAD, MANAGER, ADMIN | Verify metric |
| POST | /performance-feedback | BUYER, SOURCING_LEAD, MANAGER, ADMIN | C3 feedback intake |
| POST | /performance-feedback/{id}/verify | SOURCING_LEAD, MANAGER, ADMIN | Verify feedback |
| POST | /performance-feedback/{id}/apply-to-history | SOURCING_LEAD, MANAGER, ADMIN | Explicit apply; no auto score |
| GET | /suppliers/{id}/performance-feedback | authenticated | List feedback |
| GET | /suppliers/{id}/360 | authenticated | Includes claims, events, after-sales, feedback |
| GET | /recommendations/{id}/explanation | authenticated | Includes negotiationOpportunities |

Monetary fields: JSON numbers / strings from BigDecimal — never float.

## Questionnaire category filter + evidence summaries (post-V71)

| Method | Path | Notes |
|--------|------|-------|
| GET | /questionnaire/questions?category= | Optional QuestionCategory filter |
| GET | /suppliers/{id}/after-sales-summary | After-sales aggregate |
| GET | /suppliers/{id}/guarantee-events-summary | Guarantee event aggregate |

No Flyway required for category filter or summary endpoints (read models on existing tables).
Next free Flyway remains **V72** for next schema change.

## Dashboard KPIs — Vendor Selection counts (additive)

`GET /api/v1/dashboard/kpis` response now includes:

| Field | Meaning |
|-------|---------|
| openSupplierRisks | Risks with status OPEN |
| pendingPolicyEvidence | Policy evidence PENDING |
| openWarrantyClaims | Claims with status OPEN |
| unresolvedGuaranteeEvents | Events with resolvedFlag=false |
| pendingPerformanceFeedback | Feedback evidence PENDING |
| draftRecommendations | Recommendations DRAFT |
| submittedRecommendations | Recommendations SUBMITTED (awaiting human) |

Existing costing KPI fields unchanged. Zeros mean none recorded — not fabricated.

## Recommendation decision queue

| Method | Path | Notes |
|--------|------|-------|
| GET | /api/v1/vendor-selection/recommendations?status= | Required status: DRAFT\|SUBMITTED\|APPROVED\|REJECTED\|OVERRIDDEN |
| | | Invalid status → VS-REC-11 |

Supports human decision queue (SUBMITTED) without inventing rankings.

## V72–V85 continuous release notes (filters + pending evidence)

| Method | Path | Notes |
|--------|------|-------|
| GET | /suppliers/{id}/risks?status=&severity= | Optional RiskStatus + RiskSeverity |
| GET | /suppliers/{id}/capabilities?type=&activeOnly= | type e.g. CERTIFICATION; activeOnly default true |
| GET | /suppliers/{id}/performance-feedback?appliedToHistory=&evidenceStatus= | Filter feedback rows |
| GET | /suppliers/{id}/evidence-pending-summary | Pending counts across evidence domains |

Invalid enum query params → VS-RISK-10 / VS-CAP-10 / VS-FB-10

### Flyway
- V72 idx_sge_unresolved
- V73 idx_sr_supplier_status_severity
- V74 idx_spf_evidence_kpi
- **Next free Flyway: V75**

## V86–V90 endpoints

| Method | Path | Notes |
|--------|------|-------|
| GET | /suppliers/{id}/warranty-claims/items?claimStatus=&evidenceStatus= | Claim list filters |
| GET | /suppliers/{id}/guarantee-events/items?unresolvedOnly= | Event list filter |
| GET | /suppliers/{id}/scorecard-results | Evaluation history |
| GET | /capabilities/expiring?asOfDate= | ACTIVE with valid_to ≤ asOf (default today) |

Flyway V75: idx_swc_evidence_status. Next free: **V76**.

## V76 — Comparison history

| Method | Path | Notes |
|--------|------|-------|
| GET | /comparisons | Recent 50 snapshots (summary) |
| GET | /comparisons?bomNodeId= | Filter by BOM node |
| GET | /comparisons?programId= | Filter by program |
| GET | /comparisons/{id} | Full snapshot (existing) |

Decision support only — not supplier approval.
Flyway V76 indexes. Next free: **V77**.

## V77 — Expiring capabilities KPI
Dashboard `GET /api/v1/dashboard/kpis` includes `expiringCapabilities` (ACTIVE capabilities with valid_to ≤ today).
Flyway V77: idx_sc_status_valid_to. Next free: **V78**.

## V78 — After-sales metric list
| Method | Path | Notes |
|--------|------|-------|
| GET | /suppliers/{id}/after-sales-metrics?evidenceStatus= | List metrics; optional PENDING/VERIFIED/… |

Flyway V78: idx_sas_evidence_status. Next free: **V79**.

## V79 — Quality metrics list
| Method | Path | Notes |
|--------|------|-------|
| GET | /suppliers/{id}/quality-metrics?evidenceStatus= | Optional PENDING/VERIFIED/… |

Flyway V79: idx_sqm_evidence_status. Next free: **V80**.

## V80 — Delivery metrics list
| Method | Path | Notes |
|--------|------|-------|
| GET | /suppliers/{id}/delivery-metrics?evidenceStatus= | List delivery metrics |

(Also V79: GET /quality-metrics?evidenceStatus=)

Flyway V80: idx_sdm_evidence_status. Next free: **V81**.

## V81 — Policy list filters
| Method | Path | Notes |
|--------|------|-------|
| GET | /suppliers/{id}/policies?evidenceStatus=&policyType= | Filtered policy evidence |
| GET | /suppliers/{id}/guarantee-events/items?evidenceStatus= | Also supports unresolvedOnly |

Flyway V81: idx_sge_evidence_status. Next free: **V82**.

## V83–V86 — Transition guards
| Method | Path | Notes |
|--------|------|-------|
| POST | /warranty-claims/{id}/status | Claim commercial status graph |
| * | *verify endpoints* | Evidence status graph enforced |

WarrantyClaimSummary adds closedClaims, withdrawnClaims.
Risk updateStatus uses RiskStatusTransitionEngine.
Next free Flyway: **V87**.

## V87 — Audit logging enhancements
| Method | Path | Notes |
|--------|------|-------|
| GET | /api/v1/audit?action= | Filter TRANSITION, VERIFY, CREATE, UPDATE, DELETE |
| GET | /api/v1/audit?entityType=&action= | Combined filter |

Write API (internal): AuditService.recordTransition / recordVerify.
Flyway V87: idx_audit_entity_action_time. Next free: **V88**.

## V88 — List filter semantics (fix)
| Endpoint | Behavior |
|----------|----------|
| GET .../warranty-claims/items?claimStatus=&evidenceStatus= | **AND** when both present |
| GET .../guarantee-events/items?unresolvedOnly=&evidenceStatus= | **AND** when both present |

Flyway V88: idx_sge_resolved_evidence. Next free: **V89**.

## V89 — Completion notes
- Jackson write-bigdecimal-as-plain enabled (numeric JSON without scientific notation).
- FE performance feedback list uses existing GET .../performance-feedback filters.
- Flyway V89: claim_status and feedback evidence_status indexes. Next free: **V90**.

## Version Control — M15 (`/api/v1/versions`)

Immutable cost version snapshots (VER-01..VER-06). Money fields inside snapshot JSON are strings from BigDecimal.

| Method | Path | Roles | Notes |
|--------|------|-------|-------|
| POST | /versions | COSTING_ENGINEER, COSTING_MANAGER, PROGRAM_COST_MANAGER, ADMIN, SYSTEM_ADMINISTRATOR | Manual snapshot; changeTrigger required |
| GET | /versions/timeline?entityType=&entityId= | authenticated | Timeline newest first |
| GET | /versions/{versionId} | authenticated | Full snapshot payload |
| POST | /versions/{versionId}/baseline | COSTING_MANAGER, PROGRAM_COST_MANAGER, ADMIN | Designate program baseline |
| POST | /versions/{versionId}/rollback | COSTING_ENGINEER, COSTING_MANAGER, ADMIN | Creates **new** version with ROLLBACK trigger |
| GET | /versions/compare?entityType=&entityId=&leftVersion=&rightVersion= | authenticated | Side-by-side |

### Automated versioning (VER-01) — no extra API call required

| Domain event | Entity type | Change trigger |
|--------------|-------------|----------------|
| Should-cost generate | ShouldCostBuildUp | MANUAL_REVISION |
| Supplier quote ACCEPTED | SupplierQuote | RFQ_ROUND |
| Target cost activated | TargetCostRecord | APPROVAL_BASELINE |
| BOM node released (ECN required) | BomNode | ECN |

Entity types: `ShouldCostBuildUp`, `SupplierQuote`, `TargetCostRecord`, `BomNode`.

## V90 — Schema hardening (no API contract change)
Flyway `V90__id_column_char36_alignment.sql`: UUID ID columns on cost_version_record and core vendor-selection child tables aligned to `CHAR(36)`. No endpoint or DTO changes.

## Central Access Management (authoritative)

| Method | Path | Auth | Notes |
|--------|------|------|-------|
| GET | `/api/v1/me/access` | Authenticated | Effective customers, modules, roles |
| GET | `/api/v1/platform/modules` | SYSTEM_ADMINISTRATOR / ADMIN | Module registry |
| GET | `/api/v1/platform/customers` | SYSTEM_ADMINISTRATOR / ADMIN | List tenants |
| GET | `/api/v1/platform/customers/{id}` | SYSTEM_ADMINISTRATOR / ADMIN | Get tenant |
| GET | `/api/v1/platform/customers/{id}/modules` | SYSTEM_ADMINISTRATOR / ADMIN | Entitlements |
| GET | `/api/v1/platform/customers/{id}/users` | SYSTEM_ADMINISTRATOR / ADMIN | Memberships |
| POST | `/api/v1/platform/customers` | SYSTEM_ADMINISTRATOR / ADMIN | Create tenant |
| POST | `/api/v1/platform/entitlements` | SYSTEM_ADMINISTRATOR / ADMIN | Enable/disable module |
| POST | `/api/v1/platform/memberships` | SYSTEM_ADMINISTRATOR / ADMIN | Add user to tenant |
| PUT | `/api/v1/platform/customers/{id}` | SYSTEM_ADMINISTRATOR / ADMIN | Update customer name |
| PUT | `/api/v1/platform/customers/{id}/status` | SYSTEM_ADMINISTRATOR / ADMIN | ACTIVE / SUSPENDED / INACTIVE |
| PUT | `/api/v1/platform/memberships/{id}/deactivate` | SYSTEM_ADMINISTRATOR / ADMIN | Deactivate membership |
| * | `/api/v1/admin/users*` | SYSTEM_ADMINISTRATOR | User/role admin (existing) |

Module entitlement enforced by class-level `@RequiresModule("VENDOR_SELECTION")` on VendorSelectionController and `@RequiresModule("PRODUCT_COSTING")` on Product Costing controllers (BOM, material, process, tooling, should-cost, target, packaging, RFQ, master data, currency, ERP, reports, cost comparison, versions) + `CentralAccessService.assertModuleEnabled`.
Requires header `X-Customer-Id`. ACCESS-02..05 → HTTP 403 `MODULE_NOT_ENABLED`; ACCESS-06/07 → HTTP 403 `TENANT_ACCESS_DENIED`.


## Billing & Subscriptions (V91)

Commercial layer over Central Access. **Frontend payment success never activates modules.**

| Method | Path | Auth | Notes |
|--------|------|------|-------|
| GET | `/api/v1/billing/plans` | ADMIN+ | Active plans |
| GET | `/api/v1/billing/plans/{id}/modules` | ADMIN | Modules from catalog |
| POST | `/api/v1/billing/subscriptions` | ADMIN | Create TRIAL/PENDING |
| POST | `/api/v1/billing/subscriptions/{id}/payments` | ADMIN | Initiate gateway payment |
| GET | `/api/v1/billing/customers/{id}/subscriptions` | ADMIN | |
| GET | `/api/v1/billing/customers/{id}/invoices` | ADMIN | |
| GET | `/api/v1/billing/customers/{id}/payments` | ADMIN | |
| POST | `/api/v1/billing/webhooks/{gatewayCode}` | **permitAll** + signature | Backend-only PAID |
| POST | `/api/v1/billing/jobs/expire-subscriptions` | ADMIN | Expiry / grace |

Flow: Plan → Subscription → Invoice → Payment → Webhook (signed) → Subscription ACTIVE → CAM entitlement via `SubscriptionEntitlementService`.

## Billing access policy (V135)
- `POST /api/v1/billing/payments/{paymentId}/refunds` — record refund + access policy
- `POST /api/v1/billing/payments/{paymentId}/chargebacks` — record chargeback + access policy
- `GET /api/v1/billing/refunds` · `GET /api/v1/billing/chargebacks`
- `GET /api/v1/billing/reports/revenue-snapshot`
Policies: `KEEP_UNTIL_PERIOD_END` | `IMMEDIATE_REVOKE` | `GRACE_PERIOD`

---

## Program Cost Position (V159)

### GET /api/v1/programs/{programId}/cost-position

**Module:** PRODUCT_COSTING  
**Auth:** JWT + program scope (`AccessScopeService`)

**Query parameters**
| Name | Default | Description |
|------|---------|-------------|
| scenarioPolicy | APPROVED_THEN_CURRENT_THEN_LATEST | Or APPROVED_ONLY |
| includeDrivers | true | Include component driver breakdown |

**Behaviour**
- Selects **at most one** should-cost build-up per `bomNodeId` (never sums all scenarios).
- Selection order: APPROVED (latest) → scenario "Current Design" (latest) → latest by `calculatedAt`.
- Target: `VehicleProgram.targetCost` when set; else targetStatus = NOT_AVAILABLE.
- Variance: `shouldCostTotal - targetTotal` when both AVAILABLE; else varianceStatus = NOT_AVAILABLE.
- Missing data is **not** coerced to zero.

**Response (summary)**
- programId, programCode, modelName, reportingCurrency, asOf, selectionPolicy
- shouldCostTotal, shouldCostStatus (AVAILABLE | NOT_AVAILABLE | PARTIAL)
- targetTotal, targetStatus, targetSource (VEHICLE_PROGRAM | null)
- absoluteVariance, percentageVariance, varianceStatus
- coverage (bomNodesInProgram, nodesWithSelectedShouldCost, nodesMissingShouldCost, …)
- drivers[] (MATERIAL, PROCESS, TOOLING, …)
- dataQualityNotes[]

**Related UI:** Management Dashboard loads this when `localStorage pcs.lastProgramId` is set.


---

## Lifecycle / Volume Curve (P2-02 / V162)

### Volume curves
- `GET /api/v1/programs/{programId}/volume-curves`
- `GET /api/v1/programs/{programId}/volume-curves/{scenarioCode}`
- `PUT /api/v1/programs/{programId}/volume-curves/{scenarioCode}` — body: scenarioLabel, notes, years[]
- `POST /api/v1/programs/{programId}/volume-curves/{scenarioCode}/activate`

### Projection
- `GET /api/v1/programs/{programId}/lifecycle-projection?scenarioCode=BASE`
- Unit cost = volume-insensitive drivers + tooling re-amortized over sum(planned volumes)
- Missing baseline/curve → NOT_AVAILABLE semantics in status fields

