package com.automotive.costing.helpingagent.provider;

import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.*;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
@Primary
public class RuleBasedAIProvider implements AIProvider {

    @Override
    public List<String> selectTools(String question, ContextPayload context) {
        String q = question != null ? question.toLowerCase(Locale.ROOT) : "";
        Set<String> selected = new LinkedHashSet<>();
        String page = context != null && context.currentPage() != null ? context.currentPage().toLowerCase(Locale.ROOT) : "";

        if (containsAny(q, "bom", "tree", "hierarchy", "make/buy", "make buy", "roll-up", "rollup") || page.contains("bom")) {
            if (context != null && context.programId() != null && !context.programId().isBlank()) selected.add("get_bom_tree");
            if (context != null && context.bomNodeId() != null && !context.bomNodeId().isBlank()) selected.add("get_bom_node");
            if (selected.isEmpty()) selected.add("get_bom_tree");
        }
        if (containsAny(q, "material", "price trend", "commodity", "scrap")) selected.add("get_material_price_trend");
        if (containsAny(q, "process", "routing", "cycle time", "setup", "labor", "operation") || page.contains("process"))
            selected.add("get_process_cost_context");
        if (containsAny(q, "tooling", "die", "fixture", "amortization") || page.contains("tooling"))
            selected.add("get_tooling_cost_context");
        if (containsAny(q, "packaging", "logistics", "freight", "duty") || page.contains("packaging"))
            selected.add("get_packaging_logistics");
        if (containsAny(q, "should-cost", "should cost", "waterfall", "benchmark", "build-up", "buildup") || page.contains("should"))
            selected.add("get_should_cost");
        if (containsAny(q, "target", "variance") || page.contains("target")) {
            selected.add("get_target_cost");
            if (containsAny(q, "variance", "above target", "below target")) selected.add("get_target_variance");
        }
        if (containsAny(q, "compare", "comparison", "quote vs", "should vs") || page.contains("comparison"))
            selected.add("get_cost_comparison");
        if (containsAny(q, "version", "history", "previous version", "change from") || page.contains("version"))
            selected.add("get_cost_versions");
        if (containsAny(q, "quote", "rfq", "supplier price", "buy cost") || page.contains("rfq"))
            selected.add("get_accepted_quote");
        if (containsAny(q, "currency", "fx", "exchange rate", "conversion", "rupee", "dollar", "inr", "usd") || page.contains("currency"))
            selected.add("get_currency_context");
        if (containsAny(q, "dashboard", "kpi", "report") || page.contains("report") || page.contains("dashboard")) {
            selected.add("get_dashboard_kpis");
            if (containsAny(q, "report")) selected.add("get_report");
        }
        if (containsAny(q, "workflow", "approval", "status", "inbox")) selected.add("get_workflow_history");
        if (containsAny(q, "missing", "what is missing", "incomplete", "data quality")
                && context != null && context.bomNodeId() != null) {
            selected.add("get_bom_node"); selected.add("get_should_cost"); selected.add("get_target_cost");
        }
        if (selected.isEmpty()) {
            if (context != null && context.bomNodeId() != null && !context.bomNodeId().isBlank()) selected.add("get_bom_node");
            else if (context != null && context.programId() != null && !context.programId().isBlank()) selected.add("get_bom_tree");
            else selected.add("get_dashboard_kpis");
        }
        return List.copyOf(selected);
    }

    @Override
    public ChatResponse explain(String question, ContextPayload context, List<ToolResult> toolResults, String correlationId) {
        String q = question != null ? question.toLowerCase(Locale.ROOT) : "";
        List<SourceRef> sources = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        StringBuilder body = new StringBuilder();
        boolean anyPayload = false;
        for (ToolResult tr : toolResults) {
            if (tr.sources() != null) sources.addAll(tr.sources());
            if (tr.missingData() != null) missing.addAll(tr.missingData());
            if (!tr.success()) { failures.add(tr.toolName() + ": " + tr.errorMessage()); continue; }
            boolean hasPayload = tr.payload() != null;
            boolean onlyMissing = tr.missingData() != null && !tr.missingData().isEmpty() && !hasPayload;
            if (tr.summary() != null && !tr.summary().isBlank() && !onlyMissing) {
                body.append("• ").append(tr.summary()).append("\n");
                anyPayload = true;
            } else if (hasPayload) {
                anyPayload = true;
                if (tr.summary() != null && !tr.summary().isBlank()) {
                    body.append("• ").append(tr.summary()).append("\n");
                }
            }
        }
        boolean anySuccess = anyPayload;
        String classification; String confidence; String answer;
        if (!anySuccess && !missing.isEmpty() && failures.isEmpty()) {
            classification = "MISSING_DATA"; confidence = "HIGH";
            answer = "I could not find the Product Costing data needed to answer this question.\n"
                    + String.join("\n", missing)
                    + "\nNo values were invented. Please open the relevant screen or supply program/BOM context.";
        } else if (!anySuccess && !failures.isEmpty()) {
            classification = "MISSING_DATA"; confidence = "LOW";
            answer = "Authorized data could not be retrieved for this question.\n" + String.join("\n", failures);
        } else {
            boolean costReduction = containsAny(q, "reduce cost", "cost reduction", "lower cost", "save cost",
                    "opportunity", "investigate cost", "how can i reduce", "how to reduce");
            boolean calculatedIntent = containsAny(q, "contribute", "driver", "driving", "breakdown", "variance",
                    "compare", "difference", "increased", "decreased", "why high", "why is");
            // Prefer CALCULATED for driver/variance analysis; AI_RECOMMENDATION only for explicit reduction advice
            if (calculatedIntent && !costReduction) {
                classification = "CALCULATED";
                confidence = missing.isEmpty() ? "HIGH" : "MEDIUM";
            } else if (costReduction) {
                classification = "AI_RECOMMENDATION";
                confidence = missing.isEmpty() ? "MEDIUM" : "LOW";
            } else {
                classification = "FACT";
                confidence = missing.isEmpty() ? "HIGH" : "MEDIUM";
            }
            StringBuilder ans = new StringBuilder();
            ans.append("Based on authorized Product Costing system data (not an independent cost engine):\n");
            ans.append(body);
            if (!missing.isEmpty()) {
                ans.append("\nMISSING DATA:\n• ").append(String.join("\n• ", missing)).append("\n");
            }
            if (costReduction) {
                ans.append("\nAI RECOMMENDATION (optional investigation — no values were changed):\n");
                ans.append("• Review the largest retrieved cost component with the owning engineer.\n");
                ans.append("• Compare should-cost vs target and accepted quotes where authorized.\n");
                ans.append("• Check material price, cycle time, tooling amortization, and packaging assumptions.\n");
            }
            ans.append("\nDeterministic backend calculations remain the financial source of truth.");
            answer = ans.toString();
        }
        List<String> nextSteps = new ArrayList<>();
        nextSteps.add("Open the relevant Product Costing page for full detail and sources.");
        if (context != null && (context.programId() == null || context.programId().isBlank())
                && (context.bomNodeId() == null || context.bomNodeId().isBlank())) {
            nextSteps.add("Select a program or BOM node in the UI so the assistant can load scoped data.");
        }
        if (!missing.isEmpty()) nextSteps.add("Complete missing master data or calculations, then ask again.");
        if (containsAny(q, "reduce", "high", "variance", "above target")) {
            nextSteps.add("AI RECOMMENDATION: use Cost Comparison and Should-Cost screens to validate drivers before changing data.");
        }
        return new ChatResponse(answer.trim(), classification, confidence, List.copyOf(sources),
                List.of(), List.copyOf(missing), List.copyOf(nextSteps), correlationId);
    }

    private static boolean containsAny(String text, String... needles) {
        for (String n : needles) if (text.contains(n)) return true;
        return false;
    }
}
