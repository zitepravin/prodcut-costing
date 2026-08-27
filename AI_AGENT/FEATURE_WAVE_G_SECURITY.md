# Wave G — Security expansion (MASTER-V242)

## Delivered tests

| Suite | Coverage | Result |
|-------|----------|--------|
| PurchaseModuleGateTest | PURCHASE ACCESS-07 / ACCESS-03; PC & VS ACCESS-02 for purchase-only tenant | 5 PASS |
| PurchaseOrderSecurityTest | Cross-tenant get/submit denied; same-tenant OK; no customer context | 4 PASS |
| SupportReadOnlyFilterTest | POST /purchase-orders and .../issue blocked SUPPORT-03 | 6 PASS |
| NotificationPreferenceServiceTest | Unsubscribed module preference rejected | 2 PASS |
| RequiresModuleAspectTest | Existing VS aspect regression | 3 PASS |
| TenantIsolationHardeningTest | Suspended/inactive customer ACCESS-09 | 4 PASS |
| **Total** | | **24 PASS** |

## Enforcement (already in product code)
- `@RequiresModule("PURCHASE")` on PurchaseOrderController
- Tenant scope: `findByPurchaseOrderIdAndCustomerId`
- Support READ_ONLY filter blocks mutating methods
- Preference upsert calls `assertModuleEnabled`

## Not claimed
- Host Docker / Playwright (Wave H) — agent has no Docker daemon
