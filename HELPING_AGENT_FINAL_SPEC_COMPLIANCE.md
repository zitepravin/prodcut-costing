# Helping Agent — Final Customer Spec Compliance

**Spec:** FINAL CUSTOMER PRODUCT COSTING HELP CHATBOT SPECIFICATION  
**Implementation package:** MASTER V151  
**Principle:** Assistant only — existing Product Costing backend is the financial source of truth. No second costing engine.

## Compliance matrix

| Spec section | Requirement | Status |
|--------------|-------------|--------|
| 1–2 Purpose / questions | Natural-language PC help | **MET** — `POST /api/v1/helping-agent/chat` |
| 3 Knowledge scope | BOM…Reports | **MET** — tools map to existing services |
| 4 Behavior | Auth → tools → explain | **MET** — HelpingAgentService pipeline |
| 5 Classification | FACT / CALCULATED / RULE / AI RECOMMENDATION / AI INFERENCE / MISSING DATA | **MET** — validator allow-list includes `AI_RECOMMENDATION` (V151) |
| 6 Cost explanation | Drivers from backend data | **MET** — tool summaries; no LLM formula |
| 7 Variance | Compare via tools or MISSING DATA | **MET** — target variance + cost versions tools |
| 8 Cost reduction | Labeled recommendations only | **MET** — `AI_RECOMMENDATION` + next steps (V151) |
| 9 BOM | Read-only tree/node | **MET** — get_bom_tree, get_bom_node |
| 10 Supplier quotes | Explain only, no award | **MET** — get_accepted_quote; no write tools |
| 11–12 Auth / tenant | JWT + CAM + PRODUCT_COSTING | **MET** — AuthorizationService + `@RequiresModule` |
| 13 No direct DB | Tools → services only | **MET** |
| 14 Read-only tools | Controlled allow-list | **MET** — see tool inventory |
| 15–16 Context + UI | Page context + passive panel | **MET** — FAB opt-in; drawer |
| 17 Suggested questions | Customer prompts | **MET** — DEFAULT_PROMPTS + page prompts (V151 expanded) |
| 18–20 Errors / hallucination / injection | Safe messages; no invent | **MET** — MISSING_DATA; rule-based default |
| 21 Audit | User/tenant/tools | **MET** — HelpingAgentAuditService |
| 22 Performance | Scoped tools | **MET** — keyword tool select |
| 23–24 Tests / acceptance | Unit + write-protection | **PASS offline**; live E2E customer host |
| 25 No second engine | Service tools only | **MET** |

## Tool inventory (allow-listed, read-only)

| Tool | Purpose |
|------|---------|
| get_bom_tree | Program BOM tree |
| get_bom_node | Node detail / make-buy |
| get_material_price_trend | Material price trend |
| get_process_cost_context | Routings for node |
| get_tooling_cost_context | Tooling records |
| get_packaging_logistics | Packaging profiles |
| get_should_cost | Should-cost scenarios |
| get_target_cost | Target records |
| get_target_variance | Variance history |
| get_cost_comparison | Comparison workspace |
| get_accepted_quote | Authorized quotes |
| get_cost_versions | Version timeline |
| get_workflow_history | Approval history |
| get_dashboard_kpis | Dashboard KPIs |
| get_report | Operational / SC / variance reports |
| get_currency_context | Currencies + recent FX (**V151**) |

## Explicit non-goals (enforced)

- No BOM write / override / roll-up trigger via agent  
- No supplier approve / reject / award  
- No autonomous cost changes  
- No MySQL access from LLM  
- No cross-tenant data  
- Passive UI icon — opens only on user click  

## Tests this increment

- Helping Agent unit package: **15 PASS** (includes AI_RECOMMENDATION validator tests)
