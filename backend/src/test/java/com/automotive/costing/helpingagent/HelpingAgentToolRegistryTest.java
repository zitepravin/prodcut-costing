package com.automotive.costing.helpingagent;

import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.ContextPayload;
import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.ToolResult;
import com.automotive.costing.helpingagent.tool.HelpingAgentTool;
import com.automotive.costing.helpingagent.tool.HelpingAgentToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HelpingAgentToolRegistryTest {

    @Test
    void rejectsUnknownTool() {
        HelpingAgentToolRegistry registry = new HelpingAgentToolRegistry(List.of());
        ToolResult r = registry.invoke("drop_tables", null, Map.of());
        assertFalse(r.success());
        assertTrue(r.errorMessage().contains("not allow-listed"));
    }

    @Test
    void invokesRegisteredTool() {
        HelpingAgentTool stub = new HelpingAgentTool() {
            @Override public String name() { return "get_dashboard_kpis"; }
            @Override public String description() { return "stub"; }
            @Override public Set<String> argumentKeys() { return Set.of(); }
            @Override public ToolResult execute(ContextPayload context, Map<String, String> arguments) {
                return ToolResult.ok(name(), "ok", Map.of("k", 1), List.of());
            }
        };
        HelpingAgentToolRegistry registry = new HelpingAgentToolRegistry(List.of(stub));
        assertTrue(registry.find("get_dashboard_kpis").isPresent());
        ToolResult r = registry.invoke("GET_DASHBOARD_KPIS", null, Map.of());
        assertTrue(r.success());
        assertEquals("ok", r.summary());
    }
}
