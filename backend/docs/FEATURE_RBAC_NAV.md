# Feature: Role-based nav & path access (MASTER-V201)

## Principle
Show only features the user is allowed to use. Do not show restricted menu items and then display an access-denied banner.

## Security model (unchanged)
- **Backend** JWT roles (`@PreAuthorize`) + module entitlement = real security boundary
- **Frontend** visibility is UX only — never the sole control

## FE implementation
| File | Role |
|------|------|
| `auth/roleAccess.ts` | Path → required roles / modules; `canAccessPath()` |
| `auth/RequireRole.tsx` | Reusable page-level gate component |
| `layout/AppLayout.tsx` | Nav filter uses `canAccessPath` |

## Role-gated paths
- `/audit`, `/platform/**` → `ADMIN` or `SYSTEM_ADMINISTRATOR`

## Module-gated paths
- `/vendor-selection` → `VENDOR_SELECTION`
- `/purchase-requests` → `PURCHASE`
- Product costing routes → `PRODUCT_COSTING` (admins bypass via `hasModule`)

## Example
`demo.engineer` (COSTING_ENGINEER): no Audit Log, no Platform Admin section in sidebar.
