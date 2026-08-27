# Module independence — Purchase owns its features (MASTER-V249)

## Product style

| Module | Owns |
|--------|------|
| **PRODUCT_COSTING** | BOM, should-cost, costing sheet, programs, MvB, RFQ as costing input |
| **VENDOR_SELECTION** | Qualification, scoring, TCO, recommendation, risk |
| **PURCHASE** | PR, PO, tracking, release/export, operational P2P close |

A customer with **only PURCHASE** must run purchase end-to-end **without** BOM or VS entitlement.

## Delivered (V249)

1. `POST /api/v1/purchase-requests` — standalone PR (no `bomNodeId` / `programId`)
2. `GET /api/v1/purchase-requests` — list by tenant
3. Flyway **V129** — `program_id` / `bom_node_id` nullable
4. Supplier qualification gate **only if** `VENDOR_SELECTION` is entitled
5. Program scope check skipped when PR has no program (tenant-scoped instead)

## Optional cross-module links (not required)

| Link | When |
|------|------|
| PR from BOM node | PRODUCT_COSTING entitled + BOM path |
| Supplier APPROVED gate | VENDOR_SELECTION entitled |
| PR → RFQ | Needs program/BOM today (costing RFQ) — use **Create PO** for purchase-only |

## Non-goals unchanged

Not GR / IR / SAP live post / invented prices.
