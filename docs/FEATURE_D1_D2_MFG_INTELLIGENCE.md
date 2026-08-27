# D1–D2 Manufacturing Intelligence

**Package:** MASTER V217

## D1 Activity-Based Costing
- `activity_type` — MACHINE | LABOR | SETUP | ENERGY | QUALITY | OTHER
- Optional cost-center code + default rate
- Cost = quantity × rate (override allowed)
- Never invents rates

## D2 Capacity / Utilization
- `capacity_center` — available hours, period, efficiency %
- `capacity_load` — required hours (routing/manual)
- Utilization = required / effective hours
- Status: OK | OVERLOADED | NO_CAPACITY

## API
`/api/v1/mfg-intel/activities`  
`/api/v1/mfg-intel/activities/calculate`  
`/api/v1/mfg-intel/capacity-centers`  
`/api/v1/mfg-intel/capacity-centers/{id}/utilization`

## UI
Product Costing → Mfg Intelligence (`/mfg-intel`)
