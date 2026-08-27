package com.automotive.costing.helpingagent.service;

import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.ChatRequest;
import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.ContextPayload;
import com.automotive.costing.platform.access.CustomerContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HelpingAgentContextBuilder {
    public record RuntimeContext(String tenantId, String userId, List<String> roles,
                                 ContextPayload pageContext, String correlationId) {}

    public RuntimeContext build(ChatRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth != null ? auth.getName() : "anonymous";
        List<String> roles = auth != null
                ? auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList())
                : List.of();
        String tenantId = CustomerContext.getCustomerId();
        ContextPayload page = request.context() != null ? request.context()
                : new ContextPayload(null, null, null, null, null, null, null, null, null, null, null, null);
        String correlationId = request.correlationId() != null && !request.correlationId().isBlank()
                ? request.correlationId() : java.util.UUID.randomUUID().toString();
        return new RuntimeContext(tenantId, userId, roles, page, correlationId);
    }
}
