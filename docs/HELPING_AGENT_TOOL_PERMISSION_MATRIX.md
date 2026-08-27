# Helping Agent — Tool / Permission Matrix (Phase 0–3)

**Date:** 2026-08-14  
**Scope:** Customer-facing Product Costing Helping AI Agent (read-only)  
**Module gate:** `@RequiresModule("PRODUCT_COSTING")` on controller  
**Auth:** Authenticated + Central Access module entitlement (ACCESS-03 when disabled)

| Tool | Service source | Typical permission path | Writes |
|------|----------------|-------------------------|--------|
| get_bom_tree | BomNodeService.getTree | PRODUCT_COSTING + program scope | No |
| get_bom_node | BomNodeService.getById | PRODUCT_COSTING + program scope | No |
| get_material_price_trend | MaterialCostService.priceTrend | PRODUCT_COSTING | No |
| get_process_cost_context | ProcessRoutingService | PRODUCT_COSTING | No |
| get_tooling_cost_context | ToolingRecordService | PRODUCT_COSTING | No |
| get_packaging_logistics | PackagingLogisticsCostService | PRODUCT_COSTING | No |
| get_should_cost | ShouldCostService | PRODUCT_COSTING | No |
| get_target_cost | TargetCostingService | PRODUCT_COSTING | No |
| get_target_variance | TargetCostingService.varianceHistory | PRODUCT_COSTING | No |
| get_cost_comparison | CostComparisonService | PRODUCT_COSTING | No |
| get_accepted_quote | SupplierQuoteService.listByBomNode | PRODUCT_COSTING | No |
| get_cost_versions | CostVersionService | PRODUCT_COSTING | No |
| get_dashboard_kpis | DashboardService.kpis | Authenticated | No |
| get_report | ReportService | PRODUCT_COSTING | No |
| get_workflow_history | ApprovalWorkflowService.history | Authenticated | No |

**API:** `POST /api/v1/helping-agent/chat`  
**AI layer:** `RuleBasedAIProvider` (deterministic; no second costing engine; optional LLM provider interface reserved)  
**DB:** No new Flyway in this increment  
