# A3–A5 Cost Structure

**Package:** MASTER V210

## A3 Cost Component Structure 2.0
- Extends V92 `cost_component_definition` with `parent_component_code`, `cost_behavior` (FIXED/VARIABLE), `hierarchy_level`
- Seeded children: RAW_MATERIAL, PURCHASED_PARTS, EXTERNAL_PROCESSING under MATERIAL; DIRECT_LABOR, MACHINE, SETUP under PROCESS; FACTORY_OH, ADMIN_OH, LOGISTICS_OH under OVERHEAD
- API: `GET /api/v1/cost-structure/components/hierarchy`

## A4 Multilevel Costed BOM
- Tree from BOM subtree + latest should-cost per node (or rolled_up / override)
- Exposes qty, unit/extended cost, make/buy, material/process/tooling/pack/OH buckets, cost source
- API: `GET /api/v1/cost-structure/costed-bom/{rootBomNodeId}`

## A5 Cost Estimate Itemization
- Table `cost_estimate_item`: Qty × Rate × Source × Component → Amount
- Materialize from should-cost build-up (7 component lines)
- Fixed vs variable totals
- API: `POST /api/v1/cost-structure/itemization/should-cost/{buildUpId}`
- API: `GET /api/v1/cost-structure/itemization/{refType}/{refId}`

## UI
Product Costing → Cost Structure (`/cost-structure`)
