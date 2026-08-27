package com.automotive.costing.helpingagent.service;

import com.automotive.costing.helpingagent.audit.HelpingAgentAuditService;
import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.*;
import com.automotive.costing.helpingagent.provider.AIProvider;
import com.automotive.costing.helpingagent.tool.HelpingAgentToolRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HelpingAgentService {
    private final HelpingAgentAuthorizationService authorizationService;
    private final HelpingAgentContextBuilder contextBuilder;
    private final HelpingAgentToolRegistry toolRegistry;
    private final AIProvider aiProvider;
    private final HelpingAgentResponseValidator responseValidator;
    private final HelpingAgentAuditService auditService;

    @Transactional(readOnly = true)
    public ChatResponse chat(ChatRequest request) {
        authorizationService.assertCanUseAgent();
        var runtime = contextBuilder.build(request);
        List<String> tools = aiProvider.selectTools(request.question(), runtime.pageContext());
        auditService.recordRequest(authorizationService.currentUsername(),
                authorizationService.currentCustomerId(), runtime.correlationId(), request.question(), tools);
        List<ToolResult> results = new ArrayList<>();
        for (String toolName : tools) {
            results.add(toolRegistry.invoke(toolName, runtime.pageContext(), Map.of()));
        }
        ChatResponse raw = aiProvider.explain(request.question(), runtime.pageContext(), results, runtime.correlationId());
        return responseValidator.validate(raw);
    }
}
