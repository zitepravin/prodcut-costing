# V275 P0 STATIC CLOSURE REPORT

**Baseline:** PRODUCT-COSTING-SOFTWARE-MASTER-V275  
**Date:** 2026-08-18  
**Scope:** Static / code-level P0 only (no Docker, Redis multi-instance, k6, or live HTTP)  
**Final status:** **STATIC P0 PARTIALLY CLOSED**

---

## Status line

| Claim | Allowed? |
|-------|----------|
| HIGH-risk findById paths closed in production code | **YES** (listed below) |
| Security regression unit tests added and green | **YES** (22 tests) |
| `mvn compile` | **PASS** |
| `mvn test` full suite green | **NO** (686 run; 48 failures; 50 errors) |
| Production ready | **NO** |
| 1,000-customer ready | **NOT CLAIMED** |
| Docker E2E / Redis multi-instance / k6 | **NOT RUN** (runtime gates) |

---

## A. HIGH-risk paths closed

| Area | Change | Isolation method |
|------|--------|------------------|
| **MakeVsBuyService** | Injected `TenantAccessGuard`; `requireNode` / `requireSim` tenant-scoped; create stamps `customerId`; rebuildLegs loads build-up / quote / supplier via `findByCustomerIdAnd…`; mixed-cost requires matching BOM node (no `customer_id` column) | Tenant-scoped repo + assertOwns + BOM match |
| **LifecycleProjectionService** | Program load via `findByCustomerIdAndProgramId` | Tenant-scoped repo |
| **ShouldCostEmissionsService** | Build-up load via `findByCustomerIdAndBuildUpId` | Tenant-scoped repo |
| **BomNodeService** | Program target currency via `findByCustomerIdAndProgramId` | Tenant-scoped repo |
| **PurchaseService** | Simulation assertOwns; supplier/quote/qualification tenant-scoped | assertOwns + tenant repo |
| **SapCostEstimateService** | Should-cost compare build-up tenant-scoped | Tenant-scoped repo |
| **ScorecardService** | Restored `requireTemplate` with `findByCustomerIdAndTemplateId` | Tenant-scoped repo |
| **SupplierEvidenceService** | Import fix for `TenantAccessGuard` (already a field) | Compile integrity |
| **ShouldCostBuildUpRepository** | Restored truncated `sumTotalCostByProgramId` | Compile integrity |

### Intentionally not “blind replace”

- **EmissionFactor** `findById` — GLOBAL_REFERENCE; left as-is.  
- **User** `findById` in PurchaseService display path — PLATFORM.  
- **SAP staging** `findById` — entity has only `tenant_hint`, not `customer_id`; full isolation requires a future schema migration (documented residual, P1). Nested should-cost access is now tenant-scoped.

### Residual (not HIGH production leakage after this work)

| Item | Severity | Notes |
|------|----------|-------|
| SAP staging row by id without customer_id | MEDIUM / P1 | Schema gap; compare path no longer leaks other tenants’ should-cost |
| MixedCostVersion no customer_id | MEDIUM mitigated | Enforced BOM-node match under already tenant-owned simulation |
| Full suite test lag | Process | Outdated mocks; not production runtime defects |

---

## B. Tests added

| Test class | Tests | Result |
|------------|------:|--------|
| `HighRiskFindByIdTenantIsolationTest` | 9 | **PASS** |
| `TenantAccessGuardTest` (existing) | 5 | **PASS** |
| `TenantAccessGuardCrossTenantTest` (existing) | 3 | **PASS** |
| `QuestionnaireCategoryFilterTest` (updated for tenant queries) | 2 | **PASS** |
| `ProductionHardeningValidatorTest` (existing) | 3 | **PASS** |

**Coverage of required matrix (unit level):**

- Tenant A → Tenant A resource = allowed  
- Tenant A → Tenant B resource = denied (`assertOwns` / empty scoped query)  
- Tenant B → Tenant A resource = denied  
- Missing tenant context = hard failure (`ACCESS-TENANT-01`)  
- Nested/secondary: simulation ownership, should-cost build-up, BOM node, vehicle program  

Live HTTP matrix remains a **runtime** gate (not claimed here).

---

## C. mvn test result

```text
mvn compile          → BUILD SUCCESS
mvn test-compile     → BUILD SUCCESS
mvn test             → BUILD FAILURE
  Tests run: 686
  Failures: 48
  Errors: 50
  Skipped: 1
```

Security-focused subset (above) is fully green.

---

## D. Remaining failures (classification)

Dominant patterns after tenant hardening:

| Classification | Examples | Nature |
|----------------|----------|--------|
| **Outdated test** | Vendor evidence / risk / scorecard / recommendation InjectMocks tests still stubbing pre-tenant repository methods; ShouldCost/Tooling tests not stubbing `findByCustomerIdAnd…` | Test lag, not production logic bugs |
| **Outdated test** | `ReportServiceTest` UnnecessaryStubbing after tenant filter path | Mockito strictness |
| **Outdated test** | `ApprovalWorkflowServiceTest` instance not found after tenant require path | Needs tenant-scoped stub |
| **Production-code failure** | None confirmed from failure messages in this pass | — |
| **Environment limitation** | None for unit suite | Docker/runtime gates separate |

**Recommendation:** Next coding session should batch-update repository mocks to `findByCustomerIdAnd*` and keep `lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("test-tenant")` until suite is green. Do not revert production tenant checks to make tests pass.

---

## E. Exact files changed (production)

```text
backend/src/main/java/com/automotive/costing/makevsbuy/service/MakeVsBuyService.java
backend/src/main/java/com/automotive/costing/lifecycle/service/LifecycleProjectionService.java
backend/src/main/java/com/automotive/costing/emissions/service/ShouldCostEmissionsService.java
backend/src/main/java/com/automotive/costing/bom/service/BomNodeService.java
backend/src/main/java/com/automotive/costing/purchase/service/PurchaseService.java
backend/src/main/java/com/automotive/costing/erp/sap/service/SapCostEstimateService.java
backend/src/main/java/com/automotive/costing/shouldcost/repository/ShouldCostBuildUpRepository.java
backend/src/main/java/com/automotive/costing/vendorselection/service/SupplierEvidenceService.java
backend/src/main/java/com/automotive/costing/vendorselection/service/ScorecardService.java
```

Copies for handover: `artifacts/v275-static-p0-src/`

### Test files (representative)

```text
.../platform/access/tenant/HighRiskFindByIdTenantIsolationTest.java  (NEW)
.../vendorselection/service/QuestionnaireCategoryFilterTest.java     (UPDATED)
~40 unit tests: TenantAccessGuard @Mock + requireCustomerId stub
```

---

## F. Security implications

**Improved**

- Cross-tenant UUID guessing on Make-vs-Buy sources (should-cost, quote, supplier) no longer hydrates foreign data into legs.  
- Lifecycle / emissions / BOM program currency resolution cannot read another tenant’s program.  
- Purchase secondary paths (simulation, supplier, quote, qualification) enforce tenant.  
- SAP compare cannot attach another tenant’s should-cost build-up.  
- Scorecard templates load only within current tenant.

**Unchanged / residual**

- Application-level isolation only (no DB RLS).  
- SAP staging entity lacks `customer_id` — still a MEDIUM residual.  
- Live HTTP cross-tenant matrix still required before any production pilot.  
- Full unit suite not green → regression risk if CI is not gated on the security subset until suite is repaired.

**Compile fixes review (item 4)**

| Fix | Weakens isolation? |
|-----|--------------------|
| ShouldCostBuildUpRepository method restore | **No** — syntax only; tenant aggregates retained |
| SupplierEvidenceService import | **No** — enables existing guard field |
| ScorecardService.requireTemplate | **No** — **strengthens** isolation via customer-scoped lookup |

---

## G. Runtime gates still requiring Docker host

| Gate | Status |
|------|--------|
| P0-1 Live cross-tenant HTTP matrix | **NOT RUN** |
| P0-2 k6 scale | **NOT RUN** |
| P0-4 Multi-instance Redis / idempotency | **NOT RUN** |
| P0-5 Docker E2E + backup/restore | **NOT RUN** |

Use existing:

- `artifacts/scripts/p0-cross-tenant-http-matrix.sh`  
- `docs/production/EXTERNAL_DOCKER_TEST_INSTRUCTIONS.md`  
- `load-tests/k6-multi-tenant-smoke.js`

---

## Final status

### **STATIC P0 PARTIALLY CLOSED**

**Closed:** HIGH-risk production findById paths in prioritized modules; security unit regression suite green; compile green.  

**Not closed:** Full `mvn test` suite; all runtime P0 gates; production readiness; 1,000-customer capacity.

**Do not call V275 production-ready.**  
**Do not claim 1,000-customer support.**

---

**End of V275_P0_STATIC_CLOSURE_REPORT.md**
