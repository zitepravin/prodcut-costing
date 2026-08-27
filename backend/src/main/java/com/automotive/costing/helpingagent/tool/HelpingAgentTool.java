package com.automotive.costing.helpingagent.tool;

import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.ContextPayload;
import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.ToolResult;
import java.util.Map;
import java.util.Set;

public interface HelpingAgentTool {
    String name();
    String description();
    Set<String> argumentKeys();
    ToolResult execute(ContextPayload context, Map<String, String> arguments);
}
