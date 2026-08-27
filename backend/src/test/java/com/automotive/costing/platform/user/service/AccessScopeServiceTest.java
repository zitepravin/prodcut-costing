package com.automotive.costing.platform.user.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.platform.user.entity.ScopeType;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.entity.UserAccessScope;
import com.automotive.costing.platform.user.repository.UserAccessScopeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessScopeServiceTest {

    @Mock
    private UserAccessScopeRepository scopeRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private AccessScopeService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId("u1");
        user.setUsername("scoped");
    }

    @Test
    void unrestrictedWhenNoGrants() {
        when(scopeRepository.findByUserIdAndScopeType("u1", ScopeType.PLANT)).thenReturn(List.of());
        assertTrue(service.hasAccess(user, ScopeType.PLANT, "P1"));
        assertTrue(service.restrictedValues(user, ScopeType.PLANT).isEmpty());
    }

    @Test
    void restrictedToGrantedValues() {
        UserAccessScope s = new UserAccessScope();
        s.setScopeValue("P1");
        when(scopeRepository.findByUserIdAndScopeType("u1", ScopeType.PLANT)).thenReturn(List.of(s));
        assertTrue(service.hasAccess(user, ScopeType.PLANT, "P1"));
        assertFalse(service.hasAccess(user, ScopeType.PLANT, "P2"));
        assertThrows(AccessDeniedException.class, () -> service.assertAccess(user, ScopeType.PLANT, "P2"));
        Optional<?> r = service.restrictedValues(user, ScopeType.PLANT);
        assertTrue(r.isPresent());
    }
}
