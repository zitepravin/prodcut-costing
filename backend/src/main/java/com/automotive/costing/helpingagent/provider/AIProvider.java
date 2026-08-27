package com.automotive.costing.helpingagent.provider;

import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.ChatResponse;
import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.ContextPayload;
import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.ToolResult;
import java.util.List;

public interface AIProvider {
    List<String> selectTools(String question, ContextPayload context);
    ChatResponse explain(String question, ContextPayload context, List<ToolResult> toolResults, String correlationId);
}
