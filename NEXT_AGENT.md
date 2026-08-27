# NEXT_AGENT — V292 demo data handover

## CURRENT STATE
Master includes V167 (schema-fixed complete demo company seed) + V168 (volume extension).

## COMPLETED
- Tenant rows AURORA / HORIZON
- Extra demo users + memberships
- Schema-correct materials/machines/labor/BOM/tooling/programs in V167
- Volume materials/programs/BOM roots/should-cost/suppliers/audit in V168

## DO NOT CHANGE
- V18 EV-CX-2027 DEFAULT walkthrough data
- Existing role permission catalog
- Business logic services

## KNOWN ISSUES
- Live apply not run here
- RFQ/PO/ticket sections in V167 may need column fixes on first failure
- Routings volume incomplete vs target 15

## NEXT TASK
1. `flyway migrate` on empty MySQL matching app config
2. If V167 fails on a section, fix **only** that section’s columns (do not invent business rules)
3. Run validation counts:
   - platform_customer
   - app_user / memberships by tenant
   - material_master by customer_id
   - bom_node by customer_id
   - should_cost_build_up / target_cost_record
   - supplier_master
   - purchase_order (overdue present)
   - support_ticket by customer
4. Login smoke all demo roles
5. Dashboard KPI verify non-zero where data exists

## EXPECTED FILES
- Only Flyway SQL under `backend/src/main/resources/db/migration/`
- Status docs at repo root

## EXPECTED TESTS
- Existing unit tests unchanged
- Optional: integration test asserting min row counts after migrate

## EXPECTED DOCUMENTATION
- Update CHANGELOG on any further seed fix
