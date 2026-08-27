# Multi-Tenant Customer Management — Architecture Assessment & Implementation

**Package:** PRODUCT-COSTING-SOFTWARE-MASTER-V222  
**Date:** 2026-08-17  

---

## 1. Existing architecture assessment

The product already implements a **Central Access Management** control plane:

| Area | Existing implementation |
|------|-------------------------|
| Tenant entity | `platform_customer` / `PlatformCustomer` (`customerId`, `customerCode`, `customerName`, status) |
| Membership | `customer_user_membership` — user ↔ customer ACTIVE/INACTIVE |
| Module catalog | `platform_module` |
| Module entitlement | `customer_module_entitlement` + `@RequiresModule` aspect → ACCESS-02/03/04/05 |
| Tenant request context | `X-Customer-Id` + `CustomerContextFilter` → ACCESS-06 |
| Subscription / billing | V91 + `CustomerSubscription`, plans, TRIAL/ACTIVE/EXPIRED/SUSPENDED/CANCELLED |
| Entitlement on expiry | `SubscriptionEntitlementService` deactivates plan modules |
| Roles | SYSTEM_ADMINISTRATOR + domain roles (COSTING_*, PURCHASING_*, …) |
| Audit | `AuditService` CREATE/UPDATE/TRANSITION |
| Platform UI | `/platform/customers`, modules, users, billing, access-audit |
| Effective access | `GET /api/v1/me/access` |

**Principle already encoded:** Frontend route guards are **not** the security boundary; backend membership + module entitlement are.

---

## 2. What already existed

- Create / list / update customers  
- Activate / suspend / deactivate via status API (`ACTIVE` \| `SUSPENDED` \| `INACTIVE`)  
- Module enable/disable per customer  
- User membership add / deactivate  
- Subscription lifecycle + payments (billing package)  
- Module denial codes ACCESS-02…07  
- Platform admin pages under `/platform/*`  

---

## 3. What was missing (gaps closed in V222)

| Gap | Implementation |
|-----|----------------|
| Suspended/inactive customer still passable if membership ACTIVE | **ACCESS-09** — `assertUserInCustomer` / `assertCustomerActive` |
| Session invalidation on suspend/deactivate | Bump `tokenVersion` for all customer members |
| Controlled support access | `support_access_session` + request/approve/deny/revoke APIs |
| Platform KPI summary without costing data | `GET /api/v1/platform/dashboard-summary` |
| PLATFORM_OWNER / CUSTOMER_ADMIN role codes | Seeded in V119 |
| Explicit support path into tenant context | Filter allows approved, unexpired support session only |

---

## 4. Database changes

**Flyway `V119__support_access_and_platform_hardening.sql`**

- Table `support_access_session`  
- Roles `PLATFORM_OWNER`, `CUSTOMER_ADMIN` (if absent)  

No duplicate of `platform_customer` / entitlements / subscriptions.

---

## 5–13. Backend / security / isolation / subscription / modules / roles / audit / support

### Tenant isolation

1. User authenticates (JWT).  
2. Client sends `X-Customer-Id` only after choosing a tenant from `/me/access`.  
3. Filter: membership ACTIVE **and** customer ACTIVE, **or** approved support session.  
4. `@RequiresModule` checks entitlement (and effective dates).  
5. Business services continue to scope by domain IDs; cross-tenant blocked at membership layer.

### Platform owner confidentiality

- Platform APIs under `/api/v1/platform/**` expose **account/subscription/module/user operational** data only.  
- Business controllers require `@RequiresModule` + tenant context — platform operator **without** membership/support receives **ACCESS-06**.  
- Support access is **time-limited**, default **READ_ONLY**, audited, auto-EXPIRED.

### Subscription

- Unchanged billing states: TRIAL, ACTIVE, PAST_DUE, GRACE_PERIOD, EXPIRED, CANCELLED, SUSPENDED.  
- Expired subscription continues to drive module disable via existing entitlement service (data retained).

### Customer deactivation

- Status → SUSPENDED / INACTIVE  
- Token versions incremented → outstanding JWTs fail  
- Data retained  

---

## 14. Tests

- `TenantIsolationHardeningTest` — ACCESS-09 / ACCESS-06  

---

## 15. Build / remaining risks

| Item | Status |
|------|--------|
| Unit (isolation hardening) | Run in package step |
| Full suite / Playwright | Host-dependent — not claimed PASS here |
| Risk: admin still seeded into DEFAULT membership (V33) | Operational: remove platform operator from DEFAULT membership in production |
| Risk: support READ_ONLY not yet enforced on write endpoints | Flag set on context; recommend future write interceptor |
| Risk: granular permission codes (BOM_VIEW…) | Role-based PreAuthorize dominant today; expand matrix gradually |

---

## Most important principle (satisfied by design)

> Platform Owner controls the customer **account and subscription**;  
> customer **business data** remains tenant-isolated and confidential **by default**.


## V223 update
- Support READ_ONLY filter **SUPPORT-03** (blocks mutating methods under support session)
- V120 platform operator membership guidance recorded in Flyway history
