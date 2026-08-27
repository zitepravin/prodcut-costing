# SAP / ERP Integration Boundary

**Status:** Product rule (gap-report P0)  
**Package:** MASTER V207

## Position

This platform is a **cost-engineering and supplier-decision** system. It **complements** SAP Product Cost Planning / Controlling. It does **not** replace:

- Material Ledger / actual costing
- Inventory valuation / FI postings
- Production order settlement
- Enterprise costing runs as system of record for standard price release

## What this product owns

- Should-cost / bottom-up estimates
- Target cost and variance decision workflow
- RFQ, quotes, TCO, supplier scoring, human approval
- Make-vs-buy / mixed costing scenarios
- Business-case CM/NPV (indicative, not GL)
- SAP cost-estimate **staging and comparison**
- ERP inbound staging for master/cost payloads

## What SAP (or other ERP) owns

- Standard cost release and inventory valuation
- Actual cost collection and period-end ML
- FI/CO postings and legal books
- Official COGM/COGS for financial statements

## Integration pattern

```text
PLM/CAD (future) → Product structure
        ↓
This platform: should-cost, target, supplier decision
        ↓
ERP staging / SAP estimate compare
        ↓
SAP: valuation, actuals, postings
```

Do not implement a second Material Ledger inside this codebase.
