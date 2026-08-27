# Feature impact: tenant-scoped masters (Material / Machine / Labor / Process)

**Context:** `TABLE_TENANT_CLASSIFICATION` marks these as **tenant-owned**. V135/V136 add `customer_id` and service filters.

## Does this break existing features?

### Single-tenant / DEFAULT backfill (typical upgrade path)

| Area | Before | After | Functional effect |
|------|--------|-------|-------------------|
| Material cost calc (MAT-*) | Resolve material by **code** globally | Resolve by **(customerId, code)** | **Works** if request carries `X-Customer-Id` / membership context and legacy rows were backfilled to DEFAULT (or the real customer) |
| Process cost calc (PRC-*) | Machine/labor by code globally | By (customerId, code) + ACTIVE | **Works** under same conditions |
| BOM rollup | Uses program / node | Program/node already tenant-scoped | **Unchanged** if program belongs to tenant |
| Emissions material link | findById(recordId) | findByCustomerIdAndRecordId | **Works** with context; cross-tenant record IDs return not found (correct) |
| Dashboard counts | Global `count()` | Still global on some counters | **Cosmetic overcount** risk in multi-tenant until dashboard scoped (not a costing calc break) |

### Multi-tenant SaaS

| Behavior | Impact |
|----------|--------|
| Two customers, same material code `STL-304` | **Supported** — each has own rate/version history |
| Customer A code must not resolve for Customer B | **Enforced** — missing context or wrong tenant → not found |
| Job/scheduler without CustomerContext | **Fail closed** (`requireCustomerId`) — background jobs must set tenant context or use explicit customerId API |

### Migration risks (ops)

1. **Backfill to DEFAULT** — all pre-multi-tenant rows get `DEFAULT` customer. After go-live, re-assign or re-create under real customer if needed.
2. **Global UNIQUE on `code` / `process_name`** — older Flyway unique constraints may still be global; service layer enforces per-tenant uniqueness. Follow-up migration should replace with `(customer_id, code)` after verifying no collisions under DEFAULT.
3. **Unit tests** that call services without CustomerContext will throw — tests must set context (same as other hardened modules).

## Feature functions preserved

Costing formulas, scrap rules, index-linked pricing, routing sequence, setup amortization, multi-cavity output parts — **unchanged**. Only **data resolution scope** changed (which master row is selected).

## V266 changes for feature continuity

- `MaterialCostService` → tenant material code lookup + tenant BOM node
- `ProcessCostService` → tenant machine/labor ACTIVE lookup + tenant routing
- `EmissionFactorService` / `ShouldCostEmissionsService` → tenant material record
- `ProcessRoutingService` → tenant routing/operation create & load
- Process family `customer_id` (V136)

## Not feature-breaking by design

Isolation failures surface as **404 / not found / business rule**, not silent cross-tenant cost contamination.
