# Phase 3 — High-Risk API Permission Wiring

| Field | Value |
|-------|-------|
| **Date** | 2026-08-23 |
| **CODE** | **COMPLETE** (BOM, RFQ, Should-Cost) |
| **LIVE VERIFICATION** | **BLOCKED** |

## Scope (not a mass rewrite)

Only these controllers were converted from role-only `@PreAuthorize` to Permission Catalog checks:

| Controller | Permission codes |
|------------|------------------|
| `BomNodeController` | `BOM_VIEW`, `BOM_CREATE`, `BOM_EDIT` |
| `RfqSupplierController` | `RFQ_VIEW`, `RFQ_CREATE`, `RFQ_EDIT` (+ supplier create) |
| `ShouldCostController` | `SHOULD_COST`, `COSTING_VIEW` |

## Pattern

```java
@PreAuthorize("@permissionAuth.has('BOM_CREATE')")
```

Uses existing `PermissionAuth` → `PermissionMatrixService` Role Matrix.

Platform entitlement (`EffectivePermissionService`) can further DENY when V161 is applied; matrix remains default when no override row exists.

## Frontend

- `permissionMatrix.ts` extended with BOM / RFQ / COSTING / SHOULD_COST
- `BomPage` rollup requires `BOM_EDIT`
- UI hide is advisory; API 403 is authoritative

## Tests

- `Phase3PermissionWiringTest` (backend matrix resolution)
- `phase3PermissionWiring.test.ts` (frontend matrix)

## Explicitly not done

- Remaining ~280 role-based `@PreAuthorize` endpoints
- Full nav hide for every product page
- Live 403 dual-tenant proof
