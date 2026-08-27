# Wave A — Module entitlement architecture (MASTER-V232)

## Goal
Independent module subscription: PRODUCT_COSTING, VENDOR_SELECTION, PURCHASE.
Backend remains source of truth (`@RequiresModule` + `assertModuleEnabled`).

## Done in V232
1. FE `roleAccess.ts`: PURCHASE / VENDOR_SELECTION removed from PRODUCT_COSTING_PATHS
2. Nav: top-level **Purchase** group (only when `hasModule('PURCHASE')`)
3. Flyway **V123**: demo customers DEMO_PURCHASE_ONLY, DEMO_COSTING_ONLY, DEMO_VS_ONLY
4. Vitest: `roleAccess.test.ts` matrix checks

## Already present (unchanged)
- `RequiresModuleAspect` + `CentralAccessService.assertModuleEnabled`
- Purchase controllers `@RequiresModule("PURCHASE")`
- VS `@RequiresModule("VENDOR_SELECTION")`
- RFQ still `PRODUCT_COSTING` (Wave C decision)

## Platform Owner
Use existing Platform Modules / setEntitlement APIs to enable/disable modules.
V123 customers need user membership assigned via CAM before login tests.

## Not in Wave A
PR field expansion, PR→RFQ, PO, email engine
