package com.automotive.costing.helpingagent.tool.costing;

import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.*;
import com.automotive.costing.helpingagent.tool.HelpingAgentTool;
import com.automotive.costing.material.service.MaterialCostService;
import com.automotive.costing.process.service.ProcessRoutingService;
import com.automotive.costing.tooling.service.ToolingRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
@RequiredArgsConstructor
class GetMaterialPriceTrendTool implements HelpingAgentTool {
    private final MaterialCostService materialCostService;
    @Override public String name() { return "get_material_price_trend"; }
    @Override public String description() { return "Retrieve authorized material price trend"; }
    @Override public Set<String> argumentKeys() { return Set.of("materialCode"); }
    @Override public ToolResult execute(ContextPayload context, Map<String, String> arguments) {
        String code = first(arguments.get("materialCode"), context != null ? context.materialCode() : null);
        if (code == null || code.isBlank()) return ToolResult.missing(name(), "materialCode is required for price trend");
        try {
            return ToolResult.ok(name(), "Material price trend for " + code, materialCostService.priceTrend(code),
                    List.of(new SourceRef("COSTING", code, "Material price trend " + code)));
        } catch (Exception e) { return ToolResult.fail(name(), msg(e)); }
    }
    private static String first(String a, String b) {
        if (a != null && !a.isBlank()) return a; if (b != null && !b.isBlank()) return b; return null;
    }
    private static String msg(Exception e) { return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(); }
}

@Component
@RequiredArgsConstructor
class GetProcessRoutingTool implements HelpingAgentTool {
    private final ProcessRoutingService processRoutingService;
    @Override public String name() { return "get_process_cost_context"; }
    @Override public String description() { return "Retrieve process routing(s) for a BOM node"; }
    @Override public Set<String> argumentKeys() { return Set.of("bomNodeId", "routingId"); }
    @Override public ToolResult execute(ContextPayload context, Map<String, String> arguments) {
        String bomNodeId = first(arguments.get("bomNodeId"), context != null ? context.bomNodeId() : null);
        String routingId = first(arguments.get("routingId"), context != null ? context.routingId() : null);
        try {
            if (routingId != null && !routingId.isBlank()) {
                return ToolResult.ok(name(), "Process routing " + routingId, processRoutingService.getRouting(routingId),
                        List.of(new SourceRef("COSTING", routingId, "Process routing " + routingId)));
            }
            if (bomNodeId == null || bomNodeId.isBlank()) return ToolResult.missing(name(), "bomNodeId or routingId is required");
            var list = processRoutingService.getRoutingsForBomNode(bomNodeId);
            if (list == null || list.isEmpty()) return ToolResult.missing(name(), "No process routing for BOM node " + bomNodeId);
            return ToolResult.ok(name(), "Found " + list.size() + " routing(s) for BOM node " + bomNodeId, list,
                    List.of(new SourceRef("COSTING", bomNodeId, "Process routings for BOM node " + bomNodeId)));
        } catch (Exception e) { return ToolResult.fail(name(), msg(e)); }
    }
    private static String first(String a, String b) {
        if (a != null && !a.isBlank()) return a; if (b != null && !b.isBlank()) return b; return null;
    }
    private static String msg(Exception e) { return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(); }
}

@Component
@RequiredArgsConstructor
class GetToolingContextTool implements HelpingAgentTool {
    private final ToolingRecordService toolingRecordService;
    @Override public String name() { return "get_tooling_cost_context"; }
    @Override public String description() { return "Retrieve tooling records for a BOM node"; }
    @Override public Set<String> argumentKeys() { return Set.of("bomNodeId", "toolingId"); }
    @Override public ToolResult execute(ContextPayload context, Map<String, String> arguments) {
        String bomNodeId = first(arguments.get("bomNodeId"), context != null ? context.bomNodeId() : null);
        String toolingId = first(arguments.get("toolingId"), context != null ? context.toolingId() : null);
        try {
            if (toolingId != null && !toolingId.isBlank()) {
                return ToolResult.ok(name(), "Tooling record " + toolingId, toolingRecordService.getById(toolingId),
                        List.of(new SourceRef("COSTING", toolingId, "Tooling " + toolingId)));
            }
            if (bomNodeId == null || bomNodeId.isBlank()) return ToolResult.missing(name(), "bomNodeId or toolingId is required");
            var list = toolingRecordService.getByBomNode(bomNodeId);
            if (list == null || list.isEmpty()) return ToolResult.missing(name(), "No tooling for BOM node " + bomNodeId);
            return ToolResult.ok(name(), "Found " + list.size() + " tooling record(s) for BOM node " + bomNodeId, list,
                    List.of(new SourceRef("COSTING", bomNodeId, "Tooling for BOM node " + bomNodeId)));
        } catch (Exception e) { return ToolResult.fail(name(), msg(e)); }
    }
    private static String first(String a, String b) {
        if (a != null && !a.isBlank()) return a; if (b != null && !b.isBlank()) return b; return null;
    }
    private static String msg(Exception e) { return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(); }
}
