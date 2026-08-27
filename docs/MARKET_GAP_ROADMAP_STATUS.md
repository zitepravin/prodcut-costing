# Market Gap Roadmap Status

Source: Product_Costing_V204_Market_Benchmark_and_Gap_Report  
**Current package:** MASTER **V219**

| Priority | Capability | Status |
|----------|------------|--------|
| P0 | SAP/ERP integration boundary | **Done** (documented + C3 staging triangle; no ML clone) |
| P0 | Benchmark knowledge library | **Foundation + analytics** V105 + `/api/v1/benchmarks/analytics` (V219) |
| P0 | Profitability / business case | **Foundation** V106 + `/business-case` |
| P0 | PLM/CAD connector | **Not started** — requires customer CAD/PLM sources |
| P0 | Geometry-driven cost drivers | **Not started** — depends on CAD |
| P0 | Automated routing optimization | **Not started** — aPriori-class scope |
| P1 | Process carbon full model | **Improved V219** — PROCESS factors + process CO₂e from routing hours |
| P1 | Supplier portal FE | **Done V219** — `/supplier-portal` over existing portal APIs |
| UX | Module hierarchy | Done V205+ |

## Explicit non-goals

- Do not clone SAP CO/Material Ledger  
- Do not AI auto-approve suppliers  
- Do not treat chatbot as source of financial truth  
- Do not invent CAD geometry without connector design  

## Still blocked / external dependency

1. **PLM/CAD integration** — needs connector design + customer system choice  
2. **Geometry cost drivers / DFM loop** — depends on (1)  
3. **Global regional cost libraries at aPriori scale** — data partnership, not code only  
