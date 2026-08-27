# UI alignment with SAP worklist + market hierarchy (V220)

## SAP document UI rule
> Do not add UI screens without connecting them to a real costing capability.

All A–E / market-gap screens remain API-backed (no placeholder-only pages).

## Hierarchy rule (market quality)
```
Dashboard > Module > Page
```
Not: every page as a peer of Dashboard.

### Product Costing module children include
- Costing Variants, Quantity Structure, Cost Structure, Costing Sheets
- Advanced Costing, Scenarios, Procurement Alternatives
- Standard Costs, Financial Integration, Mfg Intelligence, Automotive Intel
- Core BOM / Process / Tooling / Should-Cost / Target / Comparison / …

### Sourcing module children include
- RFQ / Quotes, Vendor Selection, **Supplier Portal** (moved from Product Costing in V220)

### Operations
- Reports, ERP Staging, SAP Cost Estimates, Audit

## Back / breadcrumb
Shared `resolveRouteNavMeta` → parent is module landing (`/product-costing`, `/sourcing`, …), not generic Dashboard for module children.

## Mandatory fields
New forms retain `required` + `RequiredFieldLegend` per `MANDATORY_FIELD_UI_STANDARD.md`.
