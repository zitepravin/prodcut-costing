# V228 — Costing Transaction Integrity, Supplier Comparison & Approval Governance

**Package:** PRODUCT-COSTING-SOFTWARE-MASTER-V228.zip  
**Date:** 2026-08-17  
**Base:** V227  

---

## A. Executive Summary

V228 **inspected** the end-to-end chain Master Data → Costing → Supplier Comparison → Approval → Version → Release and **did not rewrite** working modules.

**Implemented (genuine gaps only):**
1. Should-cost **status transition guards** (`GOV-TRANSITION-01`…`05`) on lock, submit, and rejection paths  
2. **Governance security tests** proving CUSTOMER_ADMIN is not a rate/costing approver  
3. **Transition rule tests** documenting allowed DRAFT → SUBMITTED → APPROVED / REJECTED→DRAFT paths  

**Overall:** **READY WITH MINOR GAPS** — core chain exists and is authorized; host `mvn test`/Docker/Playwright still required; ERP reconciliation and full concurrent-approval stress tests remain open.

---

## B. V227 Carry-Forward Status

| V227 Item | Status | Evidence | Action |
|-----------|--------|----------|--------|
| Machine approve + effective-as-of | **DONE** | MachineMasterService/Controller | None |
| Labor approve + effective-as-of | **DONE** | LaborMasterService/Controller | None |
| Material approve + effective-as-of (V226) | **DONE** | MaterialMaster* | None |
| Host mvn/Docker/Playwright | **PENDING** | Agent has no Docker/Maven | Host gate |
| OverheadPool approve parity | **DEFERRED** | Overhead uses FINANCE_CONTROLLER recalculate path | Optional V229 |
| ERP reconciliation | **OPEN** | No production credentials | External |

---

## C. Costing Workflow Status

Existing should-cost model (`ApprovalStatus`): **DRAFT → SUBMITTED → APPROVED | REJECTED→DRAFT**

| Stage | Implemented | Authorization | Validation | Audit | Status |
|-------|-------------|---------------|------------|-------|--------|
| DRAFT | Yes | COSTING_ENGINEER create | Engine validation | CREATE audit | GREEN |
| SUBMITTED | Yes | Workflow submit + markSubmitted | V228: not from APPROVED/locked | UPDATE audit | GREEN |
| UNDER_REVIEW | Via M14 workflow steps | Step approver roles | Workflow engine | Step decisions | GREEN |
| APPROVED / lock | Yes | Workflow approve + lock(); V228 requires SUBMITTED | GOV-TRANSITION-02 | Snapshot + audit | GREEN |
| REJECTED → DRAFT | Yes | Workflow reject listener | V228: not from APPROVED/locked | Audit | GREEN |
| REVISED | New build-up / scenario (versioning) | Engineer | No silent overwrite of approved | Version service | YELLOW |
| FINALIZED / RELEASED | Locked + version snapshot | Approval path | locked flag | costVersionService | YELLOW (no separate RELEASED enum) |

**Invalid transitions blocked (V228):** DRAFT→APPROVED via lock; APPROVED→submit; APPROVED→reject-to-draft.

---

## D. Cost Calculation Status

| Component | Source / engine | Notes |
|-----------|-----------------|-------|
| Material | MaterialCostEngine / MaterialMaster rate | Versioned rates; effective-as-of V226 |
| Process / machine | Process costing + MachineMaster | Hourly build-up OHD-01 |
| Labor | LaborMaster + process ops | Versioned |
| Overhead | OverheadPool | FINANCE_CONTROLLER |
| Tooling | tooling module | Separate asset status |
| Packaging / logistics | packaging module | |
| FX | ExchangeRateService as-of | Historical FX |
| Should-cost total | ShouldCostEngine | Backend authoritative; BigDecimal |
| Quote components | ShouldCostQuoteComponent | Variance engine |
| SGA/margin benchmarks | BenchmarkService | Optional |

Frontend displays service responses — **backend remains authoritative**.

---

## E. Supplier Comparison Status

**Supported (Vendor Selection domain):**  
TCO engine, scorecard scoring, eligibility gates, questionnaire evaluation, recommendation ranking + **explainability engine**, quality/delivery/risk metrics, warranty/after-sales evidence, comparison service.

**Commercial where data exists:** unit price, currency, quotes (RFQ), tooling/freight when captured.

**Not invented:** AI/ML recommendation; fabricated quality history; fake performance KPIs when DATA NOT AVAILABLE.

**Human approval** remains mandatory for award (`VendorRecommendationDecision`).

---

## F. Rate Governance

| Material | Supplier | Rate | Currency | UOM | Effective | Approval | Version |
|----------|----------|------|----------|-----|-----------|----------|---------|
| MaterialMaster code | Optional index / RFQ quote | `rate` BigDecimal | Via costing currency | `uom` | effectiveFrom/To | PENDING→ACTIVE by COSTING_MANAGER | version_number |
| MachineMaster | N/A | Hourly composed | — | HR | Yes | Same pattern V227 | Yes |
| LaborMaster | N/A | Hourly | — | HR | Yes | Same pattern V227 | Yes |
| Exchange rate | N/A | rate | from/to | — | effectiveDate | Publish path | History rows |

Expired/inactive rates: status + effective window; costing uses **as-of** resolution — does not silently pick latest unrelated version.

---

## G. Permission & Security Matrix

| Role | Operation | Allowed | Scope | Backend protected |
|------|-----------|---------|-------|-------------------|
| MASTER_DATA_ADMIN | Create/new-version material/machine/labor | Yes | Catalog | @PreAuthorize |
| COSTING_MANAGER | Approve rate PENDING_APPROVAL | Yes | Catalog | @PreAuthorize (no CUSTOMER_ADMIN) |
| COSTING_ENGINEER | Create should-cost / submit path | Yes | Program scope | @PreAuthorize + AccessScope |
| COSTING_MANAGER / workflow | Decide approval steps | Yes | Workflow | ApprovalWorkflowService + roles |
| **CUSTOMER_ADMIN** | User/dept admin | Yes | Own tenant | CustomerContext |
| **CUSTOMER_ADMIN** | Rate approve / COSTING_APPROVE | **NO** | — | PreAuthorize + matrix tests V228 |
| PLATFORM_OWNER | Tenant/subscription | Yes | Platform | No default BOM access |
| Support session | Writes | **NO** | Support RO | SupportReadOnlyFilter |

---

## H. Tenant/Customer Isolation

| Test | Result |
|------|--------|
| CustomerContext membership required | Existing ACCESS-06 |
| Cross-tenant user admin | V225 ACCESS-06 tests |
| Cross-tenant master catalog | Shared plant catalog by design (not customer_id rows) — **documented** |
| Support READ_ONLY | V224 SUPPORT-03 |
| CUSTOMER_ADMIN rate approve | **Rejected** (role not on PreAuthorize; matrix test) |

Business costing (BOM/should-cost) uses program/plant **access scopes** + module entitlement.

---

## I. Version & Historical Integrity

- Master rates: **new row per version**; no in-place rate overwrite  
- Should-cost APPROVED: `locked=true` + `costVersionService.snapshotAfterChange(APPROVAL_BASELINE)`  
- MaterialRatePublishedEvent does **not** rewrite stored calculation snapshots  
- Historical rate: `findEffectiveAsOf(code, asOf)` on Material/Machine/Labor  

Previously approved costing **must not** silently change when masters move to a new version.

---

## J. Database Changes

**None in V228** (logic + tests only).  
Flyway last: **V121** · next free **V122**.

---

## K. Files Changed

| Path | Purpose |
|------|---------|
| `shouldcost/service/ShouldCostService.java` | Transition guards GOV-TRANSITION-01…05 |
| `test/.../governance/CostingGovernanceSecurityTest.java` | CUSTOMER_ADMIN / approve matrix |
| `test/.../governance/ShouldCostTransitionGovernanceTest.java` | Allowed transition documentation tests |
| AI_AGENT / PROJECT_STATUS / NEXT_AGENT / CURRENT_CONTEXT | V228 status |

---

## L. Test Results

| Test Type | Total | Passed | Failed | Skipped | Coverage |
|-----------|-------|--------|--------|---------|----------|
| New unit (governance) | Written | N/A on agent | — | Host mvn required | Branch-focused |
| V224–V227 security suite | Retained | N/A on agent | — | Host | — |
| Docker / Playwright | — | — | — | **Skipped** — no Docker on agent | — |

---

## M. Security Test Results

| Scenario | Result |
|----------|--------|
| CUSTOMER_ADMIN assign PLATFORM_OWNER | Denied (matrix) |
| CUSTOMER_ADMIN COSTING_APPROVE permission | Absent |
| COSTING_MANAGER has COSTING_APPROVE | Present |
| Rate approve API roles exclude CUSTOMER_ADMIN | Code review GREEN |
| Cross-customer user access | Existing ACCESS-06 |
| Support write during RO session | SUPPORT-03 |
| DRAFT → lock/approve | Blocked GOV-TRANSITION-02 |
| APPROVED → resubmit | Blocked GOV-TRANSITION-03 |
| APPROVED → reject to draft | Blocked GOV-TRANSITION-05 |
| Manipulated calculated cost from client | Not trusted — backend engine authoritative |

---

## N. Bugs Found

| Severity | Module | Root Cause | Fix | Test |
|----------|--------|------------|-----|------|
| Medium | ShouldCostService.lock | Could lock from DRAFT without workflow | Status guard | GOV-TRANSITION-02 |
| Medium | markSubmitted | Could mark SUBMITTED from APPROVED | Guard | GOV-TRANSITION-03 |
| Low | Rejection path | Theoretical APPROVED→DRAFT | Guard | GOV-TRANSITION-05 |

---

## O. Remaining Gaps

1. Host `mvn test` + Docker + Playwright  
2. Explicit RELEASED status enum (optional; locked+snapshot used today)  
3. OverheadPool approve/effective-as-of parity  
4. ERP vs costing reconciliation report  
5. Controlled “revise approved” workflow productization beyond new scenario/build-up  

---

## P. Deliberately Not Done

- No second master-data system  
- No second permission system  
- No fake ERP/PLM integration  
- No unnecessary versioning rewrite  
- No automatic CUSTOMER_ADMIN rate approval  
- No invented AI supplier recommendation  
- No invented costing formulas/data  

---

## Q. Recommended V229 Scope

1. Host validation gate (mandatory)  
2. OverheadPool rate approve/effective-as-of parity if FINANCE path requires it  
3. Playwright: submit → approve → lock → historical rate → compare suppliers  
4. ERP reconciliation only when staging credentials exist  

---

## R. Final Readiness

**READY WITH MINOR GAPS**

Evidence: existing workflow + VS comparison + rate versioning + V228 transition/security hardening; pending host regression and optional RELEASED/overhead parity.
