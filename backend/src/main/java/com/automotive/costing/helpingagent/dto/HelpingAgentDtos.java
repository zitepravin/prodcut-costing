package com.automotive.costing.helpingagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public final class HelpingAgentDtos {
    private HelpingAgentDtos() {}

    public record ChatRequest(
            @NotBlank @Size(max = 4000) String question,
            ContextPayload context,
            @Size(max = 64) String correlationId
    ) {}

    public record ContextPayload(
            String currentPage, String programId, String bomNodeId, String partNumber,
            String revision, String currency, String costVersionId, String buildUpId,
            String materialCode, String routingId, String toolingId, String targetCostId
    ) {}

    public record ChatResponse(
            String answer, String classification, String confidence,
            List<SourceRef> sources, List<CalculationRef> calculations,
            List<String> missingData, List<String> suggestedNextSteps, String correlationId
    ) {}

    public record SourceRef(String type, String id, String label) {}
    public record CalculationRef(String name, String value, String unit, String formulaHint) {}

    public record ToolResult(
            String toolName, boolean success, String summary, Object payload,
            List<SourceRef> sources, List<String> missingData, String errorMessage
    ) {
        public static ToolResult ok(String toolName, String summary, Object payload, List<SourceRef> sources) {
            return new ToolResult(toolName, true, summary, payload, sources, List.of(), null);
        }
        public static ToolResult missing(String toolName, String message) {
            return new ToolResult(toolName, true, message, null, List.of(), List.of(message), null);
        }
        public static ToolResult fail(String toolName, String error) {
            return new ToolResult(toolName, false, null, null, List.of(), List.of(), error);
        }
    }
}
