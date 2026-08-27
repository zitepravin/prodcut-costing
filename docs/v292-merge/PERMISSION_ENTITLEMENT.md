# Phase 2 — Permission Entitlement Implementation

| Field | Value |
|-------|-------|
| **Date** | 2026-08-23 |
| **CODE REMEDIATION** | **COMPLETE** (core platform + customer role layers) |
| **LIVE VERIFICATION** | **BLOCKED / PENDING** |
| **FINAL STATUS** | **NOT PASS / LIVE VERIFICATION PENDING** |

## Preserved (do not regress)

- Configuration pencil / ConfigEditableRow / Costing Defaults edits
- FX / Release policies locked
- Existing AuditService on costing defaults

## What was added

### Database (V161)

- `permission_catalog_entry` — Module / Feature / Function labels for existing codes
- `customer_permission_entitlement` — Platform ALLOW/DENY per customer × permission
- `customer_role_permission` — Customer Admin role grants (cannot exceed platform)

### Backend

- `EffectivePermissionService` — effective = platform AND role (matrix fallback)
- `PlatformEntitlementController` — `/api/v1/platform/...`
- `CustomerRolePermissionController` — `/api/v1/customer/role-permissions` (tenant from CustomerContext)
- `PermissionAuth` uses EffectivePermissionService

### Frontend

- `CustomerEntitlementsPage` — System Admin pencil ALLOW/DENY
- `CustomerRolePermissionsPage` — Customer Admin role matrix with 🔒 locked rows
- Routes + nav + roleAccess gates

### Not done (documented gaps)

- Mass rewrite of ~289 `@PreAuthorize` role checks
- Module pack UI pencil (existing module entitlement APIs remain)
- Full feature-level nav hide for every product page
- Live dual-tenant 403 matrix

## Golden rule

```text
platformEntitlement AND customerRolePermission AND tenantMembership → effectiveAccess
```
