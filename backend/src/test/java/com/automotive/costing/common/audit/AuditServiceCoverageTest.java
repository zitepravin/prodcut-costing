package com.automotive.costing.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceCoverageTest {

    @Mock private AuditLogRepository auditLogRepository;
    private ObjectMapper objectMapper;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        objectMapper = spy(new ObjectMapper());
        auditService = new AuditService(auditLogRepository, objectMapper);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void recordCreate_serializesNewValue() {
        auditService.recordCreate("Material", "m1", "STEEL");
        ArgumentCaptor<AuditLogEntry> cap = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(cap.capture());
        assertThat(cap.getValue().getAction()).isEqualTo("CREATE");
        assertThat(cap.getValue().getEntityType()).isEqualTo("Material");
        assertThat(cap.getValue().getUserId()).isEqualTo("system");
        assertThat(cap.getValue().getNewValue()).contains("STEEL");
    }

    @Test
    void recordUpdate_andDelete() {
        auditService.recordUpdate("Material", "m1", "OLD", "NEW");
        auditService.recordDelete("Material", "m1", "GONE");
        verify(auditLogRepository, times(2)).save(any(AuditLogEntry.class));
    }

    @Test
    void recordAuthEvent() {
        auditService.recordAuthEvent("LOGIN_SUCCESS", "user-1");
        ArgumentCaptor<AuditLogEntry> cap = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(cap.capture());
        assertThat(cap.getValue().getEntityType()).isEqualTo("AuthEvent");
        assertThat(cap.getValue().getAction()).isEqualTo("LOGIN_SUCCESS");
        assertThat(cap.getValue().getEntityId()).isEqualTo("user-1");
    }

    @Test
    void usesAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "n/a", List.of()));
        auditService.recordCreate("X", "1", null);
        ArgumentCaptor<AuditLogEntry> cap = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(cap.capture());
        assertThat(cap.getValue().getUserId()).isEqualTo("alice");
        assertThat(cap.getValue().getOldValue()).isNull();
        assertThat(cap.getValue().getNewValue()).isNull();
    }

    @Test
    void anonymousUserTreatedAsSystem() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", "n/a", List.of()));
        auditService.recordCreate("X", "1", "v");
        ArgumentCaptor<AuditLogEntry> cap = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(cap.capture());
        assertThat(cap.getValue().getUserId()).isEqualTo("system");
    }

    @Test
    void toJsonFallsBackOnSerializationFailure() throws Exception {
        ObjectMapper failing = mock(ObjectMapper.class);
        when(failing.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});
        AuditService svc = new AuditService(auditLogRepository, failing);
        svc.recordCreate("X", "1", new Object() {
            @Override public String toString() { return "fallback-obj"; }
        });
        ArgumentCaptor<AuditLogEntry> cap = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(cap.capture());
        assertThat(cap.getValue().getNewValue()).isEqualTo("fallback-obj");
    }

    @Test
    void recordsForwardedIp() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        auditService.recordCreate("X", "1", "v");
        ArgumentCaptor<AuditLogEntry> cap = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(cap.capture());
        assertThat(cap.getValue().getIpAddress()).isEqualTo("10.0.0.1");
    }

    @Test
    void recordsRemoteAddrWhenNoForwardedHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        auditService.recordCreate("X", "1", "v");
        ArgumentCaptor<AuditLogEntry> cap = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(cap.capture());
        assertThat(cap.getValue().getIpAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    void blankForwardedFallsBackToRemoteAddr() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("   ");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        auditService.recordCreate("X", "1", "v");
        ArgumentCaptor<AuditLogEntry> cap = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(cap.capture());
        assertThat(cap.getValue().getIpAddress()).isEqualTo("192.168.1.1");
    }
}
