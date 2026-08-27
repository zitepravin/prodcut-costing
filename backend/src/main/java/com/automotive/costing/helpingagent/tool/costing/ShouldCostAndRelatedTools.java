package com.automotive.costing.helpingagent.tool.costing;

import com.automotive.costing.costcomparison.service.CostComparisonService;
import com.automotive.costing.dashboard.service.DashboardService;
import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.*;
import com.automotive.costing.helpingagent.tool.HelpingAgentTool;
import com.automotive.costing.packaging.service.PackagingLogisticsCostService;
import com.automotive.costing.reports.service.ReportService;
import com.automotive.costing.rfq.service.SupplierQuoteService;
import com.automotive.costing.shouldcost.service.ShouldCostService;
import com.automotive.costing.targetcosting.service.TargetCostingService;
import com.automotive.costing.versioning.service.CostVersionService;
import com.automotive.costing.workflow.service.ApprovalWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
@RequiredArgsConstructor
class GetShouldCostTool implements HelpingAgentTool {
    private final ShouldCostService shouldCostService;
    @Override public String name() { return "get_should_cost"; }
    @Override public String description() { return "Retrieve should-cost waterfall or scenarios"; }
    @Override public Set<String> argumentKeys() { return Set.of("buildUpId", "bomNodeId"); }
    @Override public ToolResult execute(ContextPayload context, Map<String, String> arguments) {
        String buildUpId = first(arguments.get("buildUpId"), context != null ? context.buildUpId() : null);
        String bomNodeId = first(arguments.get("bomNodeId"), context != null ? context.bomNodeId() : null);
        try {
            if (buildUpId != null && !buildUpId.isBlank()) {
                return ToolResult.ok(name(), "Should-cost waterfall for build-up " + buildUpId,
                        shouldCostService.waterfall(buildUpId),
                        List.of(new SourceRef("SHOULD_COST", buildUpId, "Should-cost waterfall " + buildUpId)));
            }
            if (bomNodeId == null || bomNodeId.isBlank()) return ToolResult.missing(name(), "buildUpId or bomNodeId is required");
            var scenarios = shouldCostService.listScenarios(bomNodeId);
            if (scenarios == null || scenarios.isEmpty()) return ToolResult.missing(name(), "No should-cost scenarios for BOM node " + bomNodeId);
            return ToolResult.ok(name(), "Found " + scenarios.size() + " should-cost scenario(s)", scenarios,
                    List.of(new SourceRef("SHOULD_COST", bomNodeId, "Should-cost scenarios " + bomNodeId)));
        } catch (Exception e) { return ToolResult.fail(name(), msg(e)); }
    }
    private static String first(String a, String b) {
        if (a != null && !a.isBlank()) return a; if (b != null && !b.isBlank()) return b; return null;
    }
    private static String msg(Exception e) { return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(); }
}

@Component
@RequiredArgsConstructor
class GetTargetCostTool implements HelpingAgentTool {
    private final TargetCostingService targetCostingService;
    @Override public String name() { return "get_target_cost"; }
    @Override public String description() { return "Retrieve target cost records"; }
    @Override public Set<String> argumentKeys() { return Set.of("targetCostId", "bomNodeId", "programId"); }
    @Override public ToolResult execute(ContextPayload context, Map<String, String> arguments) {
        String targetId = first(arguments.get("targetCostId"), context != null ? context.targetCostId() : null);
        String bomNodeId = first(arguments.get("bomNodeId"), context != null ? context.bomNodeId() : null);
        String programId = first(arguments.get("programId"), context != null ? context.programId() : null);
        try {
            if (targetId != null && !targetId.isBlank()) {
                return ToolResult.ok(name(), "Target cost " + targetId, targetCostingService.getTarget(targetId),
                        List.of(new SourceRef("TARGET", targetId, "Target cost " + targetId)));
            }
            if (bomNodeId != null && !bomNodeId.isBlank()) {
                var list = targetCostingService.listByBomNode(bomNodeId);
                if (list == null || list.isEmpty()) return ToolResult.missing(name(), "No target cost for BOM node " + bomNodeId);
                return ToolResult.ok(name(), "Found " + list.size() + " target(s)", list,
                        List.of(new SourceRef("TARGET", bomNodeId, "Targets for BOM node " + bomNodeId)));
            }
            if (programId != null && !programId.isBlank()) {
                var list = targetCostingService.listByProgram(programId);
                if (list == null || list.isEmpty()) return ToolResult.missing(name(), "No target cost for program " + programId);
                return ToolResult.ok(name(), "Found " + list.size() + " target(s)", list,
                        List.of(new SourceRef("TARGET", programId, "Targets for program " + programId)));
            }
            return ToolResult.missing(name(), "targetCostId, bomNodeId, or programId is required");
        } catch (Exception e) { return ToolResult.fail(name(), msg(e)); }
    }
    private static String first(String a, String b) {
        if (a != null && !a.isBlank()) return a; if (b != null && !b.isBlank()) return b; return null;
    }
    private static String msg(Exception e) { return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(); }
}

@Component
@RequiredArgsConstructor
class GetTargetVarianceTool implements HelpingAgentTool {
    private final TargetCostingService targetCostingService;
    @Override public String name() { return "get_target_variance"; }
    @Override public String description() { return "Retrieve target variance history"; }
    @Override public Set<String> argumentKeys() { return Set.of("bomNodeId"); }
    @Override public ToolResult execute(ContextPayload context, Map<String, String> arguments) {
        String bomNodeId = first(arguments.get("bomNodeId"), context != null ? context.bomNodeId() : null);
        if (bomNodeId == null || bomNodeId.isBlank()) return ToolResult.missing(name(), "bomNodeId is required");
        try {
            var history = targetCostingService.varianceHistory(bomNodeId);
            if (history == null || history.isEmpty()) return ToolResult.missing(name(), "No variance history for BOM node " + bomNodeId);
            return ToolResult.ok(name(), "Variance history (" + history.size() + ")", history,
                    List.of(new SourceRef("TARGET", bomNodeId, "Target variance " + bomNodeId)));
        } catch (Exception e) { return ToolResult.fail(name(), msg(e)); }
    }
    private static String first(String a, String b) {
        if (a != null && !a.isBlank()) return a; if (b != null && !b.isBlank()) return b; return null;
    }
    private static String msg(Exception e) { return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(); }
}

@Component
@RequiredArgsConstructor
class GetCostComparisonTool implements HelpingAgentTool {
    private final CostComparisonService costComparisonService;
    @Override public String name() { return "get_cost_comparison"; }
    @Override public String description() { return "Cost comparison workspace"; }
    @Override public Set<String> argumentKeys() { return Set.of("bomNodeId"); }
    @Override public ToolResult execute(ContextPayload context, Map<String, String> arguments) {
        String bomNodeId = first(arguments.get("bomNodeId"), context != null ? context.bomNodeId() : null);
        if (bomNodeId == null || bomNodeId.isBlank()) return ToolResult.missing(name(), "bomNodeId is required");
        try {
            return ToolResult.ok(name(), "Cost comparison for BOM node " + bomNodeId,
                    costComparisonService.workspaceForBomNode(bomNodeId),
                    List.of(new SourceRef("COSTING", bomNodeId, "Cost comparison " + bomNodeId)));
        } catch (Exception e) { return ToolResult.fail(name(), msg(e)); }
    }
    private static String first(String a, String b) {
        if (a != null && !a.isBlank()) return a; if (b != null && !b.isBlank()) return b; return null;
    }
    private static String msg(Exception e) { return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(); }
}

@Component
@RequiredArgsConstructor
class GetPackagingLogisticsTool implements HelpingAgentTool {
    private final PackagingLogisticsCostService packagingLogisticsCostService;
    @Override public String name() { return "get_packaging_logistics"; }
    @Override public String description() { return "Packaging/logistics calculation"; }
    @Override public Set<String> argumentKeys() { return Set.of("bomNodeId"); }
    @Override public ToolResult execute(ContextPayload context, Map<String, String> arguments) {
        String bomNodeId = first(arguments.get("bomNodeId"), context != null ? context.bomNodeId() : null);
        if (bomNodeId == null || bomNodeId.isBlank()) return ToolResult.missing(name(), "bomNodeId is required");
        try {
            var latest = packagingLogisticsCostService.latestForBomNode(bomNodeId);
            if (latest == null) return ToolResult.missing(name(), "No packaging/logistics for BOM node " + bomNodeId);
            return ToolResult.ok(name(), "Latest packaging/logistics for BOM node " + bomNodeId, latest,
                    List.of(new SourceRef("COSTING", bomNodeId, "Packaging/logistics " + bomNodeId)));
        } catch (Exception e) { return ToolResult.fail(name(), msg(e)); }
    }
    private static String first(String a, String b) {
        if (a != null && !a.isBlank()) return a; if (b != null && !b.isBlank()) return b; return null;
    }
    private static String msg(Exception e) { return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(); }
}

@Component
@RequiredArgsConstructor
class GetAcceptedQuoteTool implements HelpingAgentTool {
    private final SupplierQuoteService supplierQuoteService;
    @Override public String name() { return "get_accepted_quote"; }
    @Override public String description() { return "Quotes for BOM node"; }
    @Override public Set<String> argumentKeys() { return Set.of("bomNodeId"); }
    @Override public ToolResult execute(ContextPayload context, Map<String, String> arguments) {
        String bomNodeId = first(arguments.get("bomNodeId"), context != null ? context.bomNodeId() : null);
        if (bomNodeId == null || bomNodeId.isBlank()) return ToolResult.missing(name(), "bomNodeId is required");
        try {
            var quotes = supplierQuoteService.listByBomNode(bomNodeId);
            if (quotes == null || quotes.isEmpty()) return ToolResult.missing(name(), "No quotes for BOM node " + bomNodeId);
            return ToolResult.ok(name(), "Found " + quotes.size() + " quote(s)", quotes,
                    List.of(new SourceRef("QUOTE", bomNodeId, "Quotes for BOM node " + bomNodeId)));
        } catch (Exception e) { return ToolResult.fail(name(), msg(e)); }
    }
    private static String first(String a, String b) {
        if (a != null && !a.isBlank()) return a; if (b != null && !b.isBlank()) return b; return null;
    }
    private static String msg(Exception e) { return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(); }
}

@Component
@RequiredArgsConstructor
class GetDashboardKpisTool implements HelpingAgentTool {
    private final DashboardService dashboardService;
    @Override public String name() { return "get_dashboard_kpis"; }
    @Override public String description() { return "Dashboard KPIs"; }
    @Override public Set<String> argumentKeys() { return Set.of(); }
    @Override public ToolResult execute(ContextPayload context, Map<String, String> arguments) {
        try {
            return ToolResult.ok(name(), "Dashboard KPI snapshot", dashboardService.kpis(),
                    List.of(new SourceRef("REPORT", "dashboard", "Dashboard KPIs")));
        } catch (Exception e) { return ToolResult.fail(name(), e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()); }
    }
}

@Component
@RequiredArgsConstructor
class GetReportTool implements HelpingAgentTool {
    private final ReportService reportService;
    @Override public String name() { return "get_report"; }
    @Override public String description() { return "Operational / should-cost / target-variance reports"; }
    @Override public Set<String> argumentKeys() { return Set.of("type", "programId"); }
    @Override public ToolResult execute(ContextPayload context, Map<String, String> arguments) {
        String type = arguments.getOrDefault("type", "operational").toLowerCase();
        String programId = first(arguments.get("programId"), context != null ? context.programId() : null);
        try {
            Object report; String label;
            switch (type) {
                case "should-cost-elements", "should_cost_elements" -> {
                    if (programId == null || programId.isBlank()) return ToolResult.missing(name(), "programId required");
                    report = reportService.shouldCostElements(programId); label = "Should-cost elements report";
                }
                case "target-variance", "target_variance" -> {
                    if (programId == null || programId.isBlank()) return ToolResult.missing(name(), "programId required");
                    report = reportService.targetVariance(programId); label = "Target variance report";
                }
                default -> { report = reportService.operational(programId); label = "Operational report"; }
            }
            return ToolResult.ok(name(), label, report, List.of(new SourceRef("REPORT", type, label)));
        } catch (Exception e) { return ToolResult.fail(name(), e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()); }
    }
    private static String first(String a, String b) {
        if (a != null && !a.isBlank()) return a; if (b != null && !b.isBlank()) return b; return null;
    }
}

@Component
@RequiredArgsConstructor
class GetCostVersionsTool implements HelpingAgentTool {
    private final CostVersionService costVersionService;
    @Override public String name() { return "get_cost_versions"; }
    @Override public String description() { return "Cost version timeline"; }
    @Override public Set<String> argumentKeys() { return Set.of("entityType", "entityId", "bomNodeId", "versionId"); }
    @Override public ToolResult execute(ContextPayload context, Map<String, String> arguments) {
        String versionId = first(arguments.get("versionId"), context != null ? context.costVersionId() : null);
        try {
            if (versionId != null && !versionId.isBlank()) {
                return ToolResult.ok(name(), "Cost version " + versionId, costVersionService.getVersion(versionId),
                        List.of(new SourceRef("VERSION", versionId, "Cost version " + versionId)));
            }
            String entityType = arguments.getOrDefault("entityType", CostVersionService.TYPE_BOM_NODE);
            String entityId = first(arguments.get("entityId"), arguments.get("bomNodeId"), context != null ? context.bomNodeId() : null);
            if (entityId == null || entityId.isBlank()) return ToolResult.missing(name(), "entityId/bomNodeId or versionId required");
            var timeline = costVersionService.timeline(entityType, entityId);
            if (timeline == null || timeline.isEmpty()) return ToolResult.missing(name(), "No version history");
            return ToolResult.ok(name(), "Version timeline (" + timeline.size() + ")", timeline,
                    List.of(new SourceRef("VERSION", entityId, "Version timeline " + entityId)));
        } catch (Exception e) { return ToolResult.fail(name(), e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()); }
    }
    private static String first(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }
}

@Component
@RequiredArgsConstructor
class GetWorkflowHistoryTool implements HelpingAgentTool {
    private final ApprovalWorkflowService approvalWorkflowService;
    @Override public String name() { return "get_workflow_history"; }
    @Override public String description() { return "Workflow history (read-only)"; }
    @Override public Set<String> argumentKeys() { return Set.of("entityType", "entityId", "bomNodeId"); }
    @Override public ToolResult execute(ContextPayload context, Map<String, String> arguments) {
        String entityType = arguments.getOrDefault("entityType", "BomNode");
        String entityId = first(arguments.get("entityId"), arguments.get("bomNodeId"), context != null ? context.bomNodeId() : null);
        if (entityId == null || entityId.isBlank()) return ToolResult.missing(name(), "entityId/bomNodeId required");
        try {
            var history = approvalWorkflowService.history(entityType, entityId);
            if (history == null || history.isEmpty()) return ToolResult.missing(name(), "No workflow history");
            return ToolResult.ok(name(), "Workflow history (" + history.size() + ")", history,
                    List.of(new SourceRef("COSTING", entityId, "Workflow history " + entityId)));
        } catch (Exception e) { return ToolResult.fail(name(), e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()); }
    }
    private static String first(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }
}

@Component
@RequiredArgsConstructor
class GetCurrencyContextTool implements HelpingAgentTool {
    private final com.automotive.costing.currency.service.CurrencyService currencyService;
    private final com.automotive.costing.currency.service.ExchangeRateService exchangeRateService;

    @Override public String name() { return "get_currency_context"; }
    @Override public String description() { return "Authorized currencies and recent FX rates (read-only)"; }
    @Override public Set<String> argumentKeys() { return Set.of(); }

    @Override
    public ToolResult execute(ContextPayload context, Map<String, String> arguments) {
        try {
            var currencies = currencyService.listCurrencies();
            var rates = exchangeRateService.recentRates(java.time.LocalDate.now().minusDays(30));
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("currencies", currencies);
            payload.put("recentRates", rates);
            return ToolResult.ok(name(), "Currency context (" + (currencies != null ? currencies.size() : 0)
                            + " currencies, " + (rates != null ? rates.size() : 0) + " recent rates)",
                    payload,
                    List.of(new SourceRef("COSTING", "currency", "Currency / FX")));
        } catch (Exception e) {
            return ToolResult.fail(name(), e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }
}
