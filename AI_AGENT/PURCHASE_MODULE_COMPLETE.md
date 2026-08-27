# PURCHASE MODULE — DEVELOPMENT COMPLETE

**Package:** PRODUCT-COSTING-SOFTWARE-MASTER-V251  
**Date:** 2026-08-18  
**Offline tests:** **47/47 PASS** (`com.automotive.costing.purchase.**`)

## Product rule
Each module has its own functionality. A **PURCHASE-only** tenant gets full purchase features without PRODUCT_COSTING or VENDOR_SELECTION.

| Module | This phase |
|--------|------------|
| PURCHASE | **COMPLETE** |
| PRODUCT_COSTING | Separate |
| VENDOR_SELECTION | Separate (optional gate when entitled) |
| STORE / Inventory | **FUTURE — not in scope** |

## Feature inventory

| Capability | Status |
|------------|--------|
| `@RequiresModule("PURCHASE")` | DONE |
| Standalone PR (no BOM) API + FE | DONE |
| PR lifecycle (submit/review/decide/release/cancel/reopen) | DONE |
| PR → RFQ (optional, needs program/BOM) | DONE |
| Create PO + PR→PO + externalPoReference copy | DONE |
| PO lifecycle through CLOSED | DONE |
| Tracking / delivery performance | DONE |
| SAP PO **reference** link (no live MM post) | DONE |
| Export package | DONE |
| Notifications + email queue hooks | DONE |
| Tenant isolation + support RO | DONE |
| P2P smoke scripts | DONE |

## Non-goals (locked)
Not GR · Not IR · Not SAP MM live post · No invented qty/price · Not STORE

## Host (optional)
```bash
./scripts/wave-h-host-gate.sh
CUSTOMER_ID=<uuid> ./scripts/p2p-cycle-smoke.sh
```
