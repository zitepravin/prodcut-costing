package com.automotive.costing.helpingagent;

import com.automotive.costing.helpingagent.service.HelpingAgentAuthorizationService;
import com.automotive.costing.platform.access.service.CentralAccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HelpingAgentAuthorizationServiceTest {

    @Mock
    CentralAccessService centralAccessService;

    @InjectMocks
    HelpingAgentAuthorizationService authorizationService;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deniesAnonymous() {
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class, authorizationService::assertCanUseAgent);
    }

    @Test
    void allowsAuthenticatedWithoutCustomerContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "n/a", List.of()));
        assertDoesNotThrow(authorizationService::assertCanUseAgent);
        verify(centralAccessService, never()).assertModuleEnabled(anyString(), anyString());
    }
}
