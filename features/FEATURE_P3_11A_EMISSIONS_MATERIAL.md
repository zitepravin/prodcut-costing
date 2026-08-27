# FEATURE P3-11a — Emission factors + material CO₂e on should-cost

**Package:** MASTER-V189  
**Module:** PRODUCT_COSTING

## Delivered
- V104 `emission_factor`, `material_master.default_emission_factor_id`, `should_cost_emissions_snapshot`
- Factor CRUD: create DRAFT, publish ACTIVE, retire
- Link ACTIVE factor to material master
- Compute material CO₂e = quantity_kg × factor (KG_CO2E_PER_KG)
- Missing factor/qty → `emissionsStatus = NOT_AVAILABLE` (never zero)
- Auto-compute on should-cost generate (best-effort; does not fail money path)
- GET/POST `/api/v1/should-cost/build-ups/{id}/emissions`

## Out of this slice
- Process factors (P3-11b)
- Certified LCA, carbon price, supplier EPD scoring
