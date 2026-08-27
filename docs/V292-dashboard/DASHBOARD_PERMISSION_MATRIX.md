# V292 Dashboard — Permission Matrix

| Widget / Action | Module | Permission | UI if missing | API behavior |
|-----------------|--------|------------|---------------|--------------|
| Vehicle programs | PRODUCT_COSTING | COSTING_VIEW / SHOULD_COST / BOM_VIEW | Hidden | Tenant count still computed; UI hides |
| Material masters | PRODUCT_COSTING | as above | Hidden | Tenant-scoped |
| BOM nodes | PRODUCT_COSTING | BOM_VIEW | Hidden | Tenant-scoped |
| Should-Cost records | PRODUCT_COSTING | SHOULD_COST / COSTING_VIEW | Hidden | Tenant-scoped |
| Active targets | PRODUCT_COSTING | COSTING_VIEW+ | Hidden | Tenant-scoped |
| Packaging | PRODUCT_COSTING | COSTING_VIEW+ | Hidden | Tenant-scoped |
| Pending approvals | — | (user-scoped steps) | Shown | User-scoped only |
| Unread notifications | — | (user-scoped) | Shown | User-scoped only |
| Vendor risks / warranty / recs | VENDOR_SELECTION (or RFQ roles) | RFQ / sourcing roles | Hidden | Tenant-scoped |
| Support Open / In Progress / Waiting / Resolved | — | SUPPORT_TICKET_READ | Hidden | **Backend returns 0 without permission** |
| Quick: Should-Cost | PRODUCT_COSTING | SHOULD_COST | Hidden | N/A |
| Quick: RFQ | PRODUCT_COSTING | RFQ_VIEW | Hidden | N/A |
| Quick: BOM | PRODUCT_COSTING | BOM_VIEW | Hidden | N/A |
| Quick: Support tickets | — | SUPPORT_TICKET_CREATE (or READ for link) | Hidden | N/A |
| Quick: Platform admin | — | SYSTEM_ADMINISTRATOR / ADMIN | Hidden | Platform APIs |
| Platform dashboard page | — | Platform roles | Route protected | Platform APIs |

## Module entitlement rule

If customer does not have `PRODUCT_COSTING` enabled → costing/BOM/RFQ widgets hidden even if JWT role grants permission codes.

## Tenant isolation

All dashboard counts use `TenantAccessGuard.requireCustomerId()` (or user id for personal notifications/approvals). Cross-tenant data is not returned by design of existing repositories.
