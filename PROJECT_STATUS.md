# PROJECT_STATUS — V292 (complete demo company data)

**Package:** PRODUCT-COSTING-SOFTWARE-MASTER-V292  
**Date:** 2026-08-24  
**Phase:** Multi-tenant demo data seed in master (V167 schema fix + V168 volume)

## Completed
- V167 demo seed schema alignment (materials, machines, labor, BOM, tooling, programs)
- V168 volume extension (materials, programs, products/BOMs, should-cost, suppliers, audit)
- Tenants: DEFAULT, AURORA, HORIZON retained/extended
- Role demo users (V166 + V167 extra buyers/engineers/horizon/custom)

## In progress / gaps
- Full RFQ/quotation/PR/PO volume still primarily from V167 partial sets — extend further if live KPI gaps appear
- Routings / process_routing volume not fully expanded in V168
- Customer branding binary assets not seeded (use UI StoredFileService)
- Custom roles PROCUREMENT_COORDINATOR / COSTING_ANALYST permission matrix not fully materialised beyond membership

## Known issues
- Live Flyway apply not verified in this packaging environment
- Some V167 transactional sections (RFQ/PO/tickets) may still need column-level validation on first apply

## Immediate next task
1. Apply Flyway on fresh MySQL (V1…V168)
2. Validate counts by tenant (products/BOMs/materials/RFQs/POs/tickets)
3. Role-wise login smoke (demo.* / horizon.*) password `ChangeMe!Now1`
4. Dashboard KPI spot-check against real aggregates

## Deployment
- Master ZIP contains all seed SQL inside backend Flyway path
- No separate demo package
