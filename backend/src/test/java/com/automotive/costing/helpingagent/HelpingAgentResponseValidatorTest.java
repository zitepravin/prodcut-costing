package com.automotive.costing.helpingagent;

import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.ChatResponse;
import com.automotive.costing.helpingagent.service.HelpingAgentResponseValidator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HelpingAgentResponseValidatorTest {
    private final HelpingAgentResponseValidator validator = new HelpingAgentResponseValidator();

    @Test
    void normalizesUnknownClassificationToInference() {
        ChatResponse out = validator.validate(new ChatResponse("hi", "GUESS", "maybe", List.of(), List.of(), List.of(), List.of(), "x"));
        assertEquals("AI_INFERENCE", out.classification());
        assertEquals("MEDIUM", out.confidence());
    }

    @Test
    void missingDataClassificationAddsDefaultMessageWhenEmpty() {
        ChatResponse out = validator.validate(new ChatResponse("none", "MISSING_DATA", "HIGH", List.of(), List.of(), List.of(), List.of(), "y"));
        assertEquals("MISSING_DATA", out.classification());
        assertFalse(out.missingData().isEmpty());
    }

    @Test
    void preservesValidFactResponse() {
        ChatResponse out = validator.validate(new ChatResponse("ok", "FACT", "HIGH", List.of(), List.of(), List.of(), List.of(), "z"));
        assertEquals("FACT", out.classification());
        assertEquals("HIGH", out.confidence());
        assertEquals("ok", out.answer());
    }

    @Test
    void acceptsAiRecommendationClassification() {
        ChatResponse out = validator.validate(new ChatResponse(
                "Try reviewing material drivers.", "AI_RECOMMENDATION", "MEDIUM",
                List.of(), List.of(), List.of(), List.of(), "corr-1"));
        assertEquals("AI_RECOMMENDATION", out.classification());
        assertEquals("MEDIUM", out.confidence());
    }

    @Test
    void normalizesSpacedAiRecommendation() {
        ChatResponse out = validator.validate(new ChatResponse(
                "Investigate cycle time.", "AI RECOMMENDATION", "LOW",
                List.of(), List.of(), List.of(), List.of(), "corr-2"));
        assertEquals("AI_RECOMMENDATION", out.classification());
    }
}
