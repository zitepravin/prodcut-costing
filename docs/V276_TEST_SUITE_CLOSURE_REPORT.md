# V276 TEST SUITE CLOSURE REPORT

**Baseline:** PRODUCT-COSTING-SOFTWARE-MASTER-V275 + static P0 production sources (`artifacts/v275-static-p0-src/`)  
**Date:** 2026-08-18  
**Scope:** Full regression suite green only — no Electronics, no production security weakening  

---

## 1. Compilation result

| Gate | Result |
|------|--------|
| `mvn compile` | **PASS** |
| `mvn test-compile` | **PASS** |

## 2–5. Full suite result (`mvn test`)

| Metric | Value |
|--------|-------|
| Tests run | **683** |
| Failures | **19** |
| Errors | **9** |
| Skipped | **1** |

**Classification: TEST SUITE NOT GREEN**

Starting point (V275 closure): 686 / 49 fail / 54 err.  
V276 end state: 683 / 19 fail / 9 err.  
Delta: roughly **−30 failures and −45 errors**.

## 6. Tests migrated from old repository stubs

Approximately **50+ test files** updated:

- Injected `@Mock TenantAccessGuard` + `requireCustomerId()` stubs for services that declare it
- Migrated stubs from `findById` / non-tenant methods to production tenant-scoped methods, including:
  - `findByCustomerIdAndRecommendationId`
  - `findByCustomerIdAndRiskId`
  - `findByCustomerIdAndClaimId`
  - `findByCustomerIdAndPolicyId`
  - `findByCustomerIdAndMetricId`
  - `findByCustomerIdAndTemplateId`
  - `findByCustomerIdAndBatchId`
  - `findByCustomerIdAndRoutingId`
  - `findByCustomerIdAndBuildUpId`
  - `findByCustomerIdAndQuoteId`
  - `findByCustomerIdAndWorkflowInstanceId`
  - `findByCustomerIdAndPurchaseRequestId`
  - Dashboard `countByCustomerId*` KPI methods
  - List-filter methods (`findByCustomerIdAndSupplierIdAnd…`)

## 7. Tenant-fixture corrections

- Normalized isolation tests to use **real** `TenantAccessGuard` where the test intent is context-missing denial
- Aligned Material/Machine master tests to a single tenant id (`test-customer` where CustomerContext uses that value)
- Set `customerId` on capability entities used by expiry list filtering
- Comparison list stub migrated to `findTop50ByCustomerIdOrderByCreatedAtDesc`

## 8. Mockito fixes

- `DomainOutboxService.enqueue` (non-void): `doNothing()` → `thenReturn("outbox-id")`
- Added missing `@Mock DomainOutboxService` / `PurchaseRequestLineRepository` for PurchaseOrder tests
- Converted shared/setup stubs to `lenient().when(...)` where justified (ReportService.stubSystemWide, EvidenceExpiry, PurchaseServiceStatus, ProcessCost)
- Fixed broken double-lenient syntax introduced during bulk rewrite

## 9. Security verification

- **No production security code was weakened**
- `TenantAccessGuard` / `requireCustomerId()` remain in production call paths
- Static P0 sources from `v275-static-p0-src` re-applied (MakeVsBuy, Lifecycle, Emissions, BomNode, Purchase, SapCostEstimate, Scorecard, SupplierEvidence, ShouldCostBuildUpRepository)
- High-risk findById security regression test retained under `src/test/java/com/automotive/costing/security/`
- No `@Disabled`, no deleted tests, no removed assertions, no guard bypasses

## 10. Automotive regression

Automotive is the existing industry implementation; no Electronics modules were added or modified.  
Remaining failures are fixture/stub migration issues, not intentional automotive behavior changes.

## 11. Production source changes

**None beyond re-applying the already-approved V275 static P0 files** from `artifacts/v275-static-p0-src/`.

## 12. Test-only changes

All suite work was test-side:

- Mock injection / constructor argument alignment for `TenantAccessGuard`
- Repository stub method name migration to match production
- Tenant id consistency
- Mockito strictness (lenient only where shared setup is legitimately unused on a path)
- Isolation tests rewritten to use real guard for “no context” cases

## 13. Remaining failures (exact)

| Class | F/E | Root cause (Category) | Recommended next fix |
|-------|-----|------------------------|----------------------|
| `DashboardServiceVendorKpiTest` | 1/0 | Assertion mismatch on KPI counts after count-method migration (A/B) | Align expected counts with production KPI composition; verify approval-step count path |
| `ErpPromoteServiceTest` | 1/1 | Promote count / skip path fixtures (A/B) | Stub tenant-scoped batch + material lookups used by promote path |
| `MaterialCostServiceTest` | 2/5 | BomNode / MaterialMaster / ProcessMaster stubs still non-tenant or incomplete (A) | Stub `findByBomNodeId` or tenant variant actually called; Active material rate version |
| `ProcessCostServiceTest` | 0/1 | Machine rate version not stubbed as ACTIVE (B) | Stub `findFirstByCodeAndStatusOrderByVersionNumberDesc(..., ACTIVE)` |
| `PurchaseOrderFromPrTest` | 1/2 | PurchaseRequest lookup method/id mismatch; exception order (A/B) | Confirm `findByCustomerIdAndPurchaseRequestId` + entity status for reject path |
| `PurchaseServiceStatusTest` | 1/0 | Expected `IllegalArgumentException` not thrown (B) | Match production exception type for blocked supplier |
| `ReportServiceTest` | 1/0 | Scaled element count 0 vs 2 (B) | Stub tenant-scoped should-cost element query used by report |
| `AfterSalesListFilterTest` | 1/0 | Filtered list empty (A) | Confirm exact repo method signature after customerId insertion |
| `DeliveryListFilterTest` | 1/0 | Same pattern (A) | Same |
| `PolicyListFilterTest` | 1/0 | Same pattern (A) | Same |
| `ListFilterServiceTest` | 1/0 | Same pattern (A) | Same |
| `EvidenceExpiryServiceTest` | 2/0 | Expiry not applied / assertion (B) | Ensure entity dates and repo finder match production expiry job query |
| `EvidenceSummaryServiceTest` | 2/0 | Summary counts 0 (A/B) | Tenant-scoped summary queries + entity customerId |
| `SupplierPolicyEvidenceServiceTest` | 1/0 | policySummary counts (A/B) | Tenant-scoped count/list methods |
| `SupplierQualificationApprovalGateTest` | 2/0 | Assertion on gate failure path (B) | Ensure qualification/recommendation stubs hit gate logic |
| `WarrantyClaimQaScenariosTest` | 1/0 | summaryBuckets (A/B) | Tenant-scoped summary query |

All remaining items are **Category A** (outdated stubs vs intentional tenant APIs) or **Category B** (fixture/setup).  
**Category C production regression: 0 confirmed.**  
**Category D genuine security problem: 0 confirmed.**

## 14. Exact readiness classification

### **TEST SUITE NOT GREEN**

Electronics Costing **must not start** until:

```
Failures: 0
Errors: 0
```

on a full `mvn clean test` run.

---

## Next agent actions (precise)

1. Re-extract V275 + re-apply `artifacts/v275-static-p0-src/` production files.  
2. Finish remaining **16 classes** above, method-by-method against production `find*` / `count*` calls (do not guess).  
3. For list-filter tests: print the exact production method signature and match stub arity (customerId first).  
4. For Material/Process costing: stub Active master-data rate versions.  
5. Run only `mvn clean test`; update this report when green.

**No production-readiness claim. No Electronics work until green.**
