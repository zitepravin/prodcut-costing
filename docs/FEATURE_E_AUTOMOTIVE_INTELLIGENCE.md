# Phase E — Automotive Differentiation

**Package:** MASTER V218

## E1 Cost Benchmark Library
Existing `/api/v1/benchmarks` (V105+) — packs/rates UI retained. Continuous enhancement only.

## E2 Supplier Cost Intelligence
Core COMPLETE (RFQ, TCO, 360, recommendation, human approval).

## E3 ECN Cost Impact (NEW)
- `ecn_cost_impact` table
- Baseline should-cost vs proposed build-up or manual proposed unit
- Unit delta + annual program $ (volume × unit delta)
- Never invents baseline

## E4 Central What-if (NEW)
- Material / process / tooling / FX / volume percentage shocks
- Component-aware simulation when should-cost split exists
- Annual impact with volume adjustment

## API
`/api/v1/auto-intel/ecn-impacts/analyze`  
`/api/v1/auto-intel/what-if/run`

## UI
Product Costing → Automotive Intel (`/auto-intel`)
