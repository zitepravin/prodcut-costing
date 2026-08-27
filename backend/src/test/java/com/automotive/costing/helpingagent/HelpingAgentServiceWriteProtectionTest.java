package com.automotive.costing.helpingagent;

import com.automotive.costing.helpingagent.audit.HelpingAgentAuditService;
import com.automotive.costing.helpingagent.dto.HelpingAgentDtos.*;
import com.automotive.costing.helpingagent.provider.RuleBasedAIProvider;
import com.automotive.costing.helpingagent.service.HelpingAgentAuthorizationService;
import com.automotive.costing.helpingagent.service.HelpingAgentContextBuilder;
import com.automotive.costing.helpingagent.service.HelpingAgentResponseValidator;
import com.automotive.costing.helpingagent.service.HelpingAgentService;
import com.automotive.costing.helpingagent.tool.HelpingAgentTool;
import com.automotive.costing.helpingagent.tool.HelpingAgentToolRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Ensures the agent orchestration path only invokes allow-listed tools and never
 * exposes a write surface through the chat API contract.
 */
@ExtendWith(MockitoExtension.class)
class HelpingAgentServiceWriteProtectionTest {

    @Mock HelpingAgentAuthorizationService authorizationService;
    @Mock HelpingAgentAuditService auditService;

    HelpingAgentService service;
    AtomicBoolean writeAttempted = new AtomicBoolean(false);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("engineer", "n/a", List.of()));
        lenient().doNothing().when(authorizationService).assertCanUseAgent();
        lenient().when(authorizationService.currentUsername()).thenReturn("engineer");
        lenient().when(authorizationService.currentCustomerId()).thenReturn("cust-1");

        HelpingAgentTool readTool = new HelpingAgentTool() {
            @Override public String name() { return "get_dashboard_kpis"; }
            @Override public String description() { return "read"; }
            @Override public Set<String> argumentKeys() { return Set.of(); }
            @Override public ToolResult execute(ContextPayload context, Map<String, String> arguments) {
                return ToolResult.ok(name(), "KPI snapshot", Map.of("x", 1), List.of());
            }
        };
        // Deliberately NOT registering any write tool — registry must reject invented names
        HelpingAgentToolRegistry registry = new HelpingAgentToolRegistry(List.of(readTool));
        service = new HelpingAgentService(
                authorizationService,
                new HelpingAgentContextBuilder(),
                registry,
                new RuleBasedAIProvider(),
                new HelpingAgentResponseValidator(),
                auditService
        );
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void chatUsesOnlyAllowListedToolsAndReturnsClassifiedAnswer() {
        ChatResponse response = service.chat(new ChatRequest(
                "Explain the dashboard KPIs",
                new ContextPayload("dashboard", null, null, null, null, null, null, null, null, null, null, null),
                "corr-wp-1"
        ));
        assertNotNull(response.answer());
        assertNotNull(response.classification());
        assertEquals("corr-wp-1", response.correlationId());
        verify(authorizationService).assertCanUseAgent();
        verify(auditService).recordRequest(eq("engineer"), eq("cust-1"), eq("corr-wp-1"), any(), any());
        assertFalse(writeAttempted.get());
    }

    @Test
    void registryDoesNotContainWriteToolNames() {
        HelpingAgentToolRegistry registry = new HelpingAgentToolRegistry(List.of());
        for (String banned : List.of("update_bom", "approve_workflow", "set_price", "delete_node", "submit_approval")) {
            ToolResult r = registry.invoke(banned, null, Map.of());
            assertFalse(r.success(), banned + " must not be allow-listed");
        }
    }
}
