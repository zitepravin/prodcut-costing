# V229 — Production Validation, Automated Gate & Final Costing Governance Closure

**Package:** PRODUCT-COSTING-SOFTWARE-MASTER-V229.zip  
**Date:** 2026-08-17  
**Base:** V228  

---

## A. Executive Summary

V229 executed the **host Maven gate** (after installing portable JDK 21 + Maven) and fixed **production compile/test defects** found during validation.

| Gate | Result |
|------|--------|
| Maven compile + unit tests | **PASS** — 594 tests, 0 failures, 1 skipped |
| Frontend TypeScript (`tsc -b`) | **PASS** |
| Docker compose | **NOT EXECUTED** — `docker` binary absent on agent host |
| Playwright E2E | **NOT EXECUTED** — requires running stack + Docker |
| Coverage | Jacoco report generated (see section C) |

**Release decision: READY WITH MINOR GAPS**

Reason: Backend automated suite and FE typecheck are GREEN; Docker and live Playwright remain environment gaps on this host. Cannot promote to READY without those gates.

---

## B. V228 Carry-Forward

| Item | V228 | V229 Result | Evidence |
|------|------|-------------|----------|
| Should-cost transition guards | DONE | **Regression PASS** | ShouldCostService + ShouldCostTransitionGovernanceTest (2) |
| CUSTOMER_ADMIN ≠ rate approver | DONE | **PASS** | CostingGovernanceSecurityTest (5) |
| Version snapshot integrity | DONE | **PASS** (unit) | Existing ShouldCostServiceTest (7) |
| Material/Machine/Labor approval | DONE | **PASS** | MaterialMasterServiceTest (10), MachineLaborApproveParityTest (3) |
| Effective-as-of rates | DONE | **PASS** | Material + Machine/Labor effective tests |
| Maven execution | PENDING | **EXECUTED — SUCCESS** | BUILD SUCCESS, 594 tests |
| Docker validation | PENDING | **NOT RUN** | docker not installed |
| Playwright | PENDING | **NOT RUN** | no stack |
| OverheadPool parity | DEFERRED | **OPTIONAL / NOT REQUIRED** for release | FINANCE_CONTROLLER recalculate path exists |
| ERP reconciliation | DEFERRED | **DEFERRED — EXTERNAL DEPENDENCY** | No credentials |

---

## C. Maven Results

| Metric | Result |
|--------|--------|
| Compilation | **SUCCESS** (Java 21 Temurin) |
| Tests | 594 |
| Passed | 594 − 1 skipped = **593 passed** |
| Failed | **0** |
| Errors | **0** |
| Skipped | **1** |
| Time | ~40 s |
| Coverage | Jacoco report generated under `backend/target/site/jacoco/` |

**Defects found & fixed during V229 (required for GREEN):**

1. Orphaned duplicate `@Transactional` on Material/Labor/Machine approve insert (compile error)  
2. `ShouldCostService` used `getLocked()` — boolean field requires `isLocked()`  
3. `MaterialMasterServiceTest` had tests appended outside class (test compile failure)  
4. `CentralAccessServiceTest.assertUserInCustomer_blocksNonMember` — missing ACTIVE customer mock after service began checking customer existence first  
5. `PurchaseServiceStatusTest.currentUserId` — 2-arg `UsernamePasswordAuthenticationToken` is unauthenticated in Spring Security 6  

---

## D. Docker Results

| Component | Status | Evidence |
|-----------|--------|----------|
| Backend | **NOT RUN** | `docker: command not found` |
| Frontend | **NOT RUN** | same |
| Database | **NOT RUN** | same |
| Migrations | **NOT RUN** | same |
| Health checks | **NOT RUN** | same |

**Host command when Docker available:**
```bash
export JWT_SECRET="$(openssl rand -base64 48)"
docker compose up --build -d
docker compose ps
```

---

## E. Playwright Results

| Flow | Result |
|------|--------|
| Login / Master / Costing / Comparison / Rate / Version / Security | **NOT RUN** |

Frontend typecheck: **PASS** (`npx tsc -b`).

When stack is up:
```bash
cd frontend && npm run test:e2e
```

---

## F. Security Results

| Assertion | Result |
|-----------|--------|
| CUSTOMER_ADMIN → Rate Approval → DENIED | **PASS** (matrix unit: no COSTING_APPROVE; API `@PreAuthorize` excludes CUSTOMER_ADMIN) |
| COSTING_MANAGER → Rate Approval → ALLOWED | **PASS** (matrix includes COSTING_APPROVE; API allows COSTING_MANAGER) |
| CustomerAdminBoundaryTest | **PASS** (6) |
| PrivilegeEscalationTest | **PASS** (5) |
| CrossTenantApiTest | **PASS** (5) |
| RolePermissionRegressionTest | **PASS** (6) |
| SupportReadOnlyFilterTest | **PASS** (4) |

Live API 403 proof against running server: **pending Docker**.

---

## G. Governance Results

| Guard | Result |
|-------|--------|
| GOV-TRANSITION-01…05 code present | Yes |
| ShouldCostTransitionGovernanceTest | **2/2 PASS** |
| CostingGovernanceSecurityTest | **5/5 PASS** |

---

## H. Historical Integrity

| Check | Result |
|-------|--------|
| Material approve closes prior window | **PASS** (unit) |
| findEffectiveAsOf historical rate | **PASS** (unit) |
| Machine/Labor parity | **PASS** (unit) |
| Should-cost lock + snapshot path | Existing tests PASS; full E2E pending |

---

## I. OverheadPool

**OPTIONAL / NOT REQUIRED for V229 release**  
Existing FINANCE_CONTROLLER create/recalculate path; not the same PENDING_APPROVAL material rate model. No SRS-critical gap forcing parity in this gate.

---

## J. ERP Reconciliation

**DEFERRED — EXTERNAL INTEGRATION DEPENDENCY**  
Requires live ERP credentials/endpoints. No fake reconciliation claimed.

---

## K. Bugs

| Severity | Issue | Root Cause | Fix | Test |
|----------|-------|------------|-----|------|
| High | Compile fail duplicate @Transactional | V226/V227 insert left orphan annotation | Removed orphan | Full suite |
| High | getLocked() compile error | boolean accessor is isLocked() | Replaced | Full suite |
| High | MaterialMasterServiceTest outside class | Append error | Rewrote tests inside class | 10/10 |
| Medium | CentralAccess non-member test | Service now loads customer first | Mock ACTIVE customer | CentralAccessServiceTest |
| Medium | currentUserId null | Unauthenticated token | Use 3-arg token | PurchaseServiceStatusTest |

---

## L. Files Changed (V229)

- `masterdata/service/MaterialMasterService.java` — orphan @Transactional / deactivate
- `masterdata/service/LaborMasterService.java` — same
- `masterdata/service/MachineMasterService.java` — same
- `shouldcost/service/ShouldCostService.java` — isLocked()
- `MaterialMasterServiceTest.java` — valid approve/effective tests
- `CentralAccessServiceTest.java` — customer mock
- `PurchaseServiceStatusTest.java` — authenticated token
- Governance tests retained from V228

---

## M. Remaining Gaps

1. Docker compose up + health on a Docker-capable host  
2. Playwright E2E against live stack  
3. Live REST proof of CUSTOMER_ADMIN 403 on approve endpoints  
4. ERP reconciliation (external)  

---

## N. Recommended V230

**Host-only gate (no feature work):**  
Run Docker + Playwright on customer CI/host; attach logs; promote to READY only when those pass. Do not invent new modules until the environment gate is closed.

---

## O. Final Release Decision

### READY WITH MINOR GAPS

**Evidence:** Maven **BUILD SUCCESS** (594 tests, 0 failures); FE typecheck **PASS**; governance/security unit gates **PASS**.  
**Blockers to READY:** Docker and Playwright **not executed** on this agent (tooling absent). Per V229 rules, READY requires those gates.
