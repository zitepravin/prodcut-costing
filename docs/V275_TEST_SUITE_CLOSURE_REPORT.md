# V275 TEST SUITE CLOSURE REPORT

**Baseline package:** PRODUCT-COSTING-SOFTWARE-MASTER-V275  
**Date:** 2026-08-18  
**Scope:** Make existing automated unit/integration test suite green without weakening the tenant-security model introduced in STATIC P0.  
**Constraints honored:** No new business features; no Industry Packs; no production-readiness claims; no test removal; no `@Disabled`; no assertion reduction; no bypass of `TenantAccessGuard`.

---

## 1. Initial state (from prior STATIC P0 handoff)

| Metric | Value |
|--------|-------|
| `mvn compile` | PASS |
| `mvn test-compile` | PASS (after prior constructor work) / FAIL after clean extract |
| `mvn test` | **686 tests, 48 failures, 50 errors, 1 skipped** (handoff baseline) |

Clean re-extract of V275 + re-application of STATIC P0 production patches produced constructor-mismatch compile errors in tests (expected — production constructors gained `TenantAccessGuard`).

---

## 2. Work performed

### 2.1 Production code

- Re-applied STATIC P0 source from `/home/workdir/artifacts/v275-static-p0-src/`:
  - `MakeVsBuyService`, `LifecycleProjectionService`, `ShouldCostEmissionsService`, `BomNodeService`, `PurchaseService`, `SapCostEstimateService`, `ScorecardService`, `SupplierEvidenceService`, `ShouldCostBuildUpRepository`
- **No production authorization was weakened.** All `TenantAccessGuard` / `assertOwns` / tenant-scoped repository calls retained.

### 2.2 Test-compile recovery (Category A)

Root cause: many services already had `TenantAccessGuard` in constructors (V275 baseline + STATIC P0). Tests still used pre-tenant constructors or incorrect repository method names.

Actions:
1. Automated injection of `@Mock TenantAccessGuard tenantAccessGuard` + `lenient().when(requireCustomerId()).thenReturn(...)` into affected `*ServiceTest` classes.
2. Constructor argument list alignment for services that gained `TenantAccessGuard`.
3. Corrected incorrect prior stubs:
   - `findByCustomerIdAndRecordId` → `findById` where production still uses `findById` (BusinessCase, CostingVariant, Notification, StandardCost, CostVersion).
   - `findTop20ByCustomerIdOrderByCreatedAtDesc` → `findTop50ByCustomerIdOrderByCreatedAtDesc`.
   - Questionnaire category method name alignment.
4. Fixed non-void `DomainOutboxService.enqueue` stubs (`doNothing` → `thenReturn("outbox-id")`) in PurchaseOrder* tests.

**Result:** `mvn test-compile` → **BUILD SUCCESS** (confirmed multiple times).

### 2.3 Runtime test runs (measured)

| Run | Tests | Failures | Errors | Skipped | Notes |
|-----|-------|----------|--------|---------|-------|
| Post constructor + basic stub fixes | 686 | 20 | 38 | 1 | Improved vs 48/50 |
| After broader TAG mock injection | 686 | 35 | 69 | 1 | Temporary regression (NPE on uninjected InjectMocks) |
| After InjectMocks mock + stub migration | 686 | 49 | 54 | 1 | ResourceNotFound dominant |

Dominant remaining failure class (Category **A**):

```
ResourceNotFoundException: <Entity> not found with id: <id>
```

Top entities (counts from surefire):

| Entity | Approx. count | Cause |
|--------|---------------|--------|
| ErpInboundBatch | 9 | Test stubs `findById` / non-tenant; production uses `findByCustomerIdAndBatchId` |
| BomNode | 9 | Mixed `findByBomNodeId` vs tenant-scoped stubs |
| VendorRecommendation | 7 | `findById` → needs `findByCustomerIdAndRecommendationId` |
| ProcessRouting | 6 | `findById` → `findByRoutingId` / tenant variant |
| SupplierPolicyEvidence | 5 | tenant-scoped lookup |
| PurchaseRequest | 4 | tenant-scoped lookup |
| SupplierWarrantyClaim / SupplierRisk | 3 each | tenant-scoped lookup |
| ShouldCostBuildUp / MaterialMaster / MachineMaster | various | tenant-scoped / method-name mismatch |

Other residual classes:

- **UnnecessaryStubbingException** (~11): strict Mockito; need `lenient()` on unused stubs.
- **AssertionError / expected exception type mismatch**: tests expect `BusinessRuleViolationException` but receive `ResourceNotFoundException` because tenant lookup fails first.
- **NullPointerException** on `tenantAccessGuard` / `domainOutboxService`: a few remaining `@InjectMocks` / reflection-constructed services still missing the mock.

**No Category C (production regression) or Category D (genuine security hole) was confirmed.** Failures are test fixtures lagging intentional tenant-security API changes.

---

## 3. Classification summary (all observed failures)

| Category | Description | Count (approx.) | Action taken / required |
|----------|-------------|-----------------|-------------------------|
| **A** | Outdated test caused by intentional tenant-security API/repository changes | ~90+ of residual F+E | Update stubs to `findByCustomerIdAnd*` / correct method names; inject `TenantAccessGuard` mock; align tenant id strings (`test-tenant` vs `test-customer`) |
| **B** | Test fixture/setup problem | subset of A + UnnecessaryStubbing + doNothing-on-non-void | Fixed enqueue; remaining need lenient + correct arg matchers |
| **C** | Production-code regression | **0 confirmed** | — |
| **D** | Genuine security problem | **0 confirmed** | — |
| **E** | Unrelated / environment | Session `/tmp` wipe, Maven/JDK path loss mid-session | Re-extract + re-apply patches required; not a product defect |

---

## 4. Files changed (this session)

### Production (re-applied from STATIC P0 archive only)
- `makevsbuy/service/MakeVsBuyService.java`
- `lifecycle/service/LifecycleProjectionService.java`
- `emissions/service/ShouldCostEmissionsService.java`
- `bom/service/BomNodeService.java`
- `purchase/service/PurchaseService.java`
- `erp/sap/service/SapCostEstimateService.java`
- `shouldcost/repository/ShouldCostBuildUpRepository.java`
- `vendorselection/service/ScorecardService.java`
- `vendorselection/service/SupplierEvidenceService.java`

### Tests (representative; ~50+ files touched by automation)
- Constructor / `@Mock TenantAccessGuard` / `requireCustomerId` stubs across masterdata, process, material, tooling, shouldcost, erp, purchase, reports, workflow, vendorselection service tests.
- Repository method name corrections (BusinessCase, CostingVariant, Notification, StandardCost, CostVersion, ComparisonList, Questionnaire, MaterialMaster, MachineMaster, Tooling, QuantityStructure, Erp*, Vendor* evidence/risk/claim/recommendation tests).
- `DomainOutboxService.enqueue` non-void stub fix in PurchaseOrder* tests.

---

## 5. Final measured result (last complete run before environment loss)

```
Tests run: 686
Failures: 49
Errors: 54
Skipped: 1
BUILD FAILURE
```

`mvn test-compile`: **PASS**  
`mvn clean test`: **NOT GREEN**

---

## 6. Acceptance assessment

| Criterion | Status |
|-----------|--------|
| Preferred: BUILD SUCCESS, 0 failures, 0 errors | **NOT MET** |
| Do not remove / disable tests | Met |
| Do not weaken TenantAccessGuard | Met |
| Do not claim PASS without evidence | Met |
| Classify every failure | Met (all residual are Category A/B) |
| Explain remaining items | Met |

**Final status: TEST SUITE PARTIALLY CLOSED**

- Compile + test-compile green.
- Security unit paths from STATIC P0 remain in place.
- Remaining failures are **outdated test fixtures** (Category A) that still call pre-tenant repository methods or omit `TenantAccessGuard` mocks.
- No evidence of production regression or security bypass introduced by test work.

---

## 7. Exact next steps for the following agent

1. Re-extract V275, re-apply `/home/workdir/artifacts/v275-static-p0-src/*` production files.
2. Re-apply (or re-run) the TenantAccessGuard `@Mock` + constructor alignment for all 43 services that declare `private final TenantAccessGuard`.
3. Systematically replace remaining `findById(id)` stubs with the tenant-scoped method used by the corresponding service (table in §2.3).
4. Normalize tenant id strings: either all `"test-tenant"` or all `"test-customer"` — match what `requireCustomerId()` returns in that test’s `@BeforeEach`.
5. Convert residual `UnnecessaryStubbingException` to `lenient().when(...)`.
6. Run `mvn clean test` (full suite only).
7. Update this report with final counts when green.

---

## 8. Risk statement

Leaving Category A tests red does **not** indicate a production tenant-isolation regression. It indicates the automated suite has not yet been fully migrated to the tenant-aware repository contracts introduced for STATIC P0. Production HIGH-risk `findById` paths addressed in STATIC P0 remain closed.

**Do not claim production readiness or full test-suite closure from this report.**
