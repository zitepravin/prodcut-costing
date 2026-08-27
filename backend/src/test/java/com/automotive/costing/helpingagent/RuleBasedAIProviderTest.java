package com.automotive.costing.helpingagent;

import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.*;
import com.automotive.costing.helpingagent.provider.RuleBasedAIProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleBasedAIProviderTest {
    private final RuleBasedAIProvider provider = new RuleBasedAIProvider();

    @Test
    void selectsBomToolsForBomQuestion() {
        ContextPayload ctx = new ContextPayload("Bom", "prog-1", "node-1", null, null, null, null, null, null, null, null, null);
        List<String> tools = provider.selectTools("Explain this BOM tree", ctx);
        assertTrue(tools.contains("get_bom_tree") || tools.contains("get_bom_node"));
    }

    @Test
    void selectsShouldCostForWaterfallQuestion() {
        ContextPayload ctx = new ContextPayload("ShouldCost", null, "node-1", null, null, null, null, "bu-1", null, null, null, null);
        assertTrue(provider.selectTools("How is the should-cost built?", ctx).contains("get_should_cost"));
    }

    @Test
    void selectsCurrencyToolForFxQuestion() {
        List<String> tools = provider.selectTools("What FX rates are available?", null);
        assertTrue(tools.contains("get_currency_context"), "expected get_currency_context, got " + tools);
    }

    @Test
    void explainMarksMissingDataWhenToolsReportMissing() {
        List<ToolResult> results = List.of(ToolResult.missing("get_bom_node", "bomNodeId is required"));
        ChatResponse response = provider.explain("Why is cost high?", null, results, "corr-1");
        assertEquals("MISSING_DATA", response.classification());
        assertFalse(response.missingData().isEmpty());
        assertEquals("corr-1", response.correlationId());
    }

    @Test
    void explainUsesFactWhenToolReturnsData() {
        List<ToolResult> results = List.of(ToolResult.ok("get_dashboard_kpis", "Dashboard KPI snapshot", new Object(),
                List.of(new SourceRef("REPORT", "dashboard", "Dashboard KPIs"))));
        ChatResponse response = provider.explain("Explain the dashboard", null, results, "c2");
        assertEquals("FACT", response.classification());
        assertEquals("HIGH", response.confidence());
        assertFalse(response.sources().isEmpty());
        assertTrue(response.answer().contains("authorized Product Costing"));
    }

    @Test
    void explainUsesAiRecommendationForCostReductionQuestion() {
        List<ToolResult> results = List.of(ToolResult.ok("get_should_cost", "Should-cost scenarios", new Object(),
                List.of(new SourceRef("COSTING", "sc-1", "Should-cost"))));
        ChatResponse response = provider.explain("How can I investigate cost reduction?", null, results, "c3");
        assertEquals("AI_RECOMMENDATION", response.classification());
        assertTrue(response.answer().toUpperCase().contains("RECOMMENDATION")
                || response.suggestedNextSteps().stream().anyMatch(s -> s.toUpperCase().contains("RECOMMENDATION")));
        assertFalse(response.answer().toLowerCase().contains("i recalculated"));
    }

    @Test
    void explainUsesCalculatedForDriverBreakdownQuestion() {
        List<ToolResult> results = List.of(ToolResult.ok("get_bom_node", "Node detail", new Object(),
                List.of(new SourceRef("BOM", "n1", "BOM node"))));
        ChatResponse response = provider.explain("What is driving the cost breakdown variance?", null, results, "c4");
        assertEquals("CALCULATED", response.classification());
    }
}
