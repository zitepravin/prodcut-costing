package com.automotive.costing.helpingagent.provider;

import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.ChatResponse;
import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.ContextPayload;
import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Optional external LLM adapter. Disabled by default.
 * When enabled without a valid endpoint/key, falls back behavior is delegated by keeping
 * RuleBasedAIProvider as {@code @Primary}; this bean is only constructed when property is true
 * and is intended for future injection swap / explicit selection — not auto-replacing Primary yet.
 *
 * <p>Does not invent financial values: any future LLM path must still receive tool results only.
 */
@Component
@ConditionalOnProperty(prefix = "helping-agent.llm", name = "enabled", havingValue = "true")
public class HttpAIProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(HttpAIProvider.class);

    private final RuleBasedAIProvider fallback;
    private final String endpoint;
    private final String apiKey;

    public HttpAIProvider(
            RuleBasedAIProvider fallback,
            @Value("${helping-agent.llm.endpoint:}") String endpoint,
            @Value("${helping-agent.llm.api-key:}") String apiKey) {
        this.fallback = fallback;
        this.endpoint = endpoint != null ? endpoint.trim() : "";
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        if (this.endpoint.isEmpty() || this.apiKey.isEmpty()) {
            log.warn("helping-agent.llm.enabled=true but endpoint/api-key incomplete — using rule-based fallback only");
        } else {
            log.info("HttpAIProvider configured (endpoint set); tool-grounded answers still required");
        }
    }

    @Override
    public List<String> selectTools(String question, ContextPayload context) {
        // Tool selection remains deterministic and allow-listed
        return fallback.selectTools(question, context);
    }

    @Override
    public ChatResponse explain(String question, ContextPayload context, List<ToolResult> toolResults, String correlationId) {
        // Phase-safe: do not call external LLM until credentials + policy are production-approved.
        // Always ground on tool results via rule-based explainer.
        return fallback.explain(question, context, toolResults, correlationId);
    }
}
