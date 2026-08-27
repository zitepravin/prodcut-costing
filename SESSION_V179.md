# SESSION_V179 — P3-01 Hardening C/E/F/G + Live-gate support

**Package:** PRODUCT-COSTING-SOFTWARE-MASTER-V179.zip  
**Authorization:** AUTHORIZE P3-01 HARDENING Scope: C, E, F, G and AUTHORIZE LIVE-GATE SUPPORT

## Changes
- C: applyTcoSnapshot from complete SupplierTcoCalculation
- E: tryStartWorkflow + PURCHASE_REQUEST_SUBMITTED notification; NotificationType enum extended
- F: AppLayout filter hasModule('PURCHASE') for /purchase-requests
- G: PurchaseServiceStatusTest Mockito coverage for A + C
- CUSTOMER_LIVE_GATE_HANDOFF.md §10 Phase-3 purchase addendum
- features/FEATURE_P3_01_HARDENING_CEFG.md

## Schema
- No new Flyway (V98 remains latest)

## Policy unchanged
- No AI approve/release; no invented FX; no SAP MM post; no RELEASE on NOT_AVAILABLE
