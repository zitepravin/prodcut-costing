package com.automotive.costing.helpingagent.tool;

import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.ContextPayload;
import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.ToolResult;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class HelpingAgentToolRegistry {
    private final Map<String, HelpingAgentTool> tools = new LinkedHashMap<>();

    public HelpingAgentToolRegistry(List<HelpingAgentTool> toolBeans) {
        for (HelpingAgentTool tool : toolBeans) {
            tools.put(tool.name().toLowerCase(Locale.ROOT), tool);
        }
    }

    public Optional<HelpingAgentTool> find(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        return Optional.ofNullable(tools.get(name.toLowerCase(Locale.ROOT)));
    }

    public Collection<HelpingAgentTool> all() { return List.copyOf(tools.values()); }
    public Set<String> names() { return Set.copyOf(tools.keySet()); }

    public ToolResult invoke(String toolName, ContextPayload context, Map<String, String> arguments) {
        HelpingAgentTool tool = find(toolName).orElse(null);
        if (tool == null) return ToolResult.fail(toolName, "Tool not allow-listed: " + toolName);
        return tool.execute(context, arguments != null ? Map.copyOf(arguments) : Map.of());
    }
}
