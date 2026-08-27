# B4 Scenario Costing + B5 Additive Costs

**Package:** MASTER V213

## B4 Scenario / Version Costing
- Table `costing_scenario` with typed scenarios:
  BASE, SUPPLIER_A/B, MAKE, BUY, MIXED, ENGINEERING_CHANGE, VOLUME_CHANGE,
  PLANT_CHANGE, MATERIAL_CHANGE, CURRENCY_CHANGE, CUSTOM
- Status: DRAFT → ACTIVE → APPROVED (locked) | SUPERSEDED
- **Never overwrite APPROVED** — use `/branch` to create a new DRAFT child
- Optional link to should-cost build-up and costing variant
- API: `/api/v1/costing-scenarios`

## B5 Additive Costing
- Table `additive_cost_line` on a scenario
- Types: PROTOTYPE, LAUNCH_PREMIUM, SPECIAL_TOOLING, ONE_TIME_ENGINEERING,
  SPECIAL_FREIGHT, CUSTOMER_SPECIFIC, OTHER
- Optional amortization → amount_per_unit
- Blocked on APPROVED/locked scenarios

## UI
Product Costing → Scenarios (`/scenarios`)
