package com.automotive.costing.helpingagent.service;

import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.ChatResponse;
import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.SourceRef;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class HelpingAgentResponseValidator {
    private static final Set<String> CLASSIFICATIONS = Set.of(
            "FACT", "CALCULATED", "RULE_RESULT", "AI_RECOMMENDATION", "AI_INFERENCE", "MISSING_DATA");
    private static final Set<String> CONFIDENCES = Set.of("HIGH", "MEDIUM", "LOW");

    public ChatResponse validate(ChatResponse raw) {
        String classification = normalizeClassification(raw.classification());
        String confidence = normalizeConfidence(raw.confidence());
        List<SourceRef> sources = raw.sources() != null ? List.copyOf(raw.sources()) : List.of();
        List<String> missing = raw.missingData() != null ? List.copyOf(raw.missingData()) : List.of();
        List<String> next = raw.suggestedNextSteps() != null ? List.copyOf(raw.suggestedNextSteps()) : List.of();
        String answer = raw.answer() != null ? raw.answer() : "No answer generated.";
        if ("MISSING_DATA".equals(classification) && missing.isEmpty()) {
            missing = List.of("Required Product Costing data is unavailable or not authorized for this request.");
        }
        return new ChatResponse(answer, classification, confidence, sources,
                raw.calculations() != null ? List.copyOf(raw.calculations()) : List.of(),
                missing, next, raw.correlationId());
    }

    private static String normalizeClassification(String value) {
        if (value == null || value.isBlank()) return "AI_INFERENCE";
        String u = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if ("AI_RECOMMENDATION".equals(u) || "RECOMMENDATION".equals(u)) return "AI_RECOMMENDATION";
        return CLASSIFICATIONS.contains(u) ? u : "AI_INFERENCE";
    }

    private static String normalizeConfidence(String value) {
        if (value == null || value.isBlank()) return "MEDIUM";
        String u = value.trim().toUpperCase(Locale.ROOT);
        return CONFIDENCES.contains(u) ? u : "MEDIUM";
    }
}
