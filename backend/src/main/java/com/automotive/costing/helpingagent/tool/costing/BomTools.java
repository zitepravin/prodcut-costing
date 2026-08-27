package com.automotive.costing.helpingagent.tool.costing;

import com.automotive.costing.bom.dto.BomNodeDtos;
import com.automotive.costing.bom.service.BomNodeService;
import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.ContextPayload;
import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.SourceRef;
import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.ToolResult;
import com.automotive.costing.helpingagent.tool.HelpingAgentTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
class GetBomTreeTool implements HelpingAgentTool {
    private final BomNodeService bomNodeService;
    @Override public String name() { return "get_bom_tree"; }
    @Override public String description() { return "Retrieve authorized BOM tree for a vehicle program"; }
    @Override public Set<String> argumentKeys() { return Set.of("programId"); }
    @Override
    public ToolResult execute(ContextPayload context, Map<String, String> arguments) {
        String programId = first(arguments.get("programId"), context != null ? context.programId() : null);
        if (programId == null || programId.isBlank()) return ToolResult.missing(name(), "programId is required to load the BOM tree");
        try {
            List<BomNodeDtos.TreeNode> tree = bomNodeService.getTree(programId);
            if (tree == null || tree.isEmpty()) return ToolResult.missing(name(), "No BOM nodes found for program " + programId);
            return ToolResult.ok(name(), "BOM tree for program " + programId + " contains " + count(tree) + " node(s).", tree,
                    List.of(new SourceRef("BOM", programId, "BOM tree for program " + programId)));
        } catch (Exception e) {
            return ToolResult.fail(name(), msg(e));
        }
    }
    private static int count(List<BomNodeDtos.TreeNode> nodes) {
        if (nodes == null) return 0;
        int n = 0;
        for (BomNodeDtos.TreeNode node : nodes) n += 1 + count(node.children());
        return n;
    }
    private static String first(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }
    private static String msg(Exception e) { return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(); }
}

@Component
@RequiredArgsConstructor
class GetBomNodeTool implements HelpingAgentTool {
    private final BomNodeService bomNodeService;
    @Override public String name() { return "get_bom_node"; }
    @Override public String description() { return "Retrieve a single BOM node detail including make/buy and cost fields"; }
    @Override public Set<String> argumentKeys() { return Set.of("bomNodeId", "nodeId"); }
    @Override
    public ToolResult execute(ContextPayload context, Map<String, String> arguments) {
        String nodeId = first(arguments.get("bomNodeId"), arguments.get("nodeId"), context != null ? context.bomNodeId() : null);
        if (nodeId == null || nodeId.isBlank()) return ToolResult.missing(name(), "bomNodeId is required to load node detail");
        try {
            BomNodeDtos.Response node = bomNodeService.getById(nodeId);
            String summary = "BOM node " + nodeId
                    + (node.partNumber() != null ? " part " + node.partNumber() : "")
                    + (node.makeBuyFlag() != null ? " make/buy=" + node.makeBuyFlag() : "");
            return ToolResult.ok(name(), summary, node, List.of(new SourceRef("BOM", nodeId, "BOM node " + nodeId)));
        } catch (Exception e) {
            return ToolResult.fail(name(), msg(e));
        }
    }
    private static String first(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }
    private static String msg(Exception e) { return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(); }
}
