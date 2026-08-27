# B1–B3 Advanced Costing

**Package:** MASTER V212

## B1 Costing Run / Mass Costing
- Tables: `costing_run`, `costing_run_item`
- Select by program and/or explicit BOM node IDs
- Execute runs quantity-structure estimate per node (A2)
- Success/failure counts, error log per item, rerun-failed
- API: `/api/v1/costing-runs` + `/execute` + `/rerun-failed`

## B2 Lot-Size Costing
- Standard volumes: 1, 10, 100, 1,000, 10,000, 100,000 (or custom)
- Unit cost curve via A2 engine (setup amortization)
- Reports lowest unit-cost lot size among successful points
- API: `POST /api/v1/lot-size-costing/curve`

## B3 Unified Variance Engine
- Pure driver math: MATERIAL, PROCESS, TOOLING, PACKAGING, OVERHEAD, SGA, MARGIN
- Baseline/comparison: SHOULD_COST, PREVIOUS_SHOULD_COST, TARGET, QUOTE/ADHOC
- Does not invent actuals
- API: `POST /api/v1/variance/compare`

## UI
Product Costing → Advanced Costing (`/advanced-costing`)
