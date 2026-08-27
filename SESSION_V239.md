# SESSION_V239 — V127 migrate readiness + PO lifecycle smoke evidence

## Agent environment
- Docker: **not available** (cannot run live Flyway against MySQL or compose image build here)
- Offline evidence:
  - `PurchaseOrderLifecycleTest` 3 PASS (create→submit→decide→issue + NOT_AVAILABLE block + ACCESS-07)
  - `PurchaseOrderStatusTest` 7 PASS
  - Total 10 PASS BUILD SUCCESS

## Host (required for live V127)
1. `docker compose up --build -d`  (Flyway applies through **V127** on backend start)
2. Confirm PURCHASE entitlement (DEFAULT via V122, or Platform Modules)
3. `CUSTOMER_ID=<uuid> ./scripts/purchase-order-smoke.sh`

## Scripts added
- `scripts/purchase-order-smoke.sh`
- `scripts/v238-host-po-gate.sh`
