package com.automotive.costing.platform.user.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.config.OidcProperties;
import com.automotive.costing.platform.user.dto.OidcDtos.LinkIdentityRequest;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.repository.OidcIdentityLinkRepository;
import com.automotive.costing.platform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OidcIdentityServiceTest {

    @Mock private OidcIdentityLinkRepository linkRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private OidcProperties oidcProperties;

    @InjectMocks
    private OidcIdentityService service;

    @Test
    void link_setsSsoSubjectWhenEmpty() {
        User u = new User();
        u.setUserId("u1");
        u.setSsoSubject(null);
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        when(linkRepository.existsByIssuerAndSubjectAndStatus(any(), any(), any())).thenReturn(false);
        when(linkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.link(new LinkIdentityRequest("u1", "https://idp.example", "sub-9", "a@b.com"));
        assertThat(resp.subject()).isEqualTo("sub-9");
        assertThat(u.getSsoSubject()).isEqualTo("sub-9");
    }

    @Test
    void link_rejectsDuplicateIssuerSubject() {
        User u = new User();
        u.setUserId("u1");
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        when(linkRepository.existsByIssuerAndSubjectAndStatus(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.link(new LinkIdentityRequest("u1", "iss", "sub", null)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
