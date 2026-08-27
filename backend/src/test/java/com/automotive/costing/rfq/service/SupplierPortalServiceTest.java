package com.automotive.costing.rfq.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.notification.service.NotificationService;
import com.automotive.costing.platform.user.repository.UserRepository;
import com.automotive.costing.rfq.repository.PortalEventLogRepository;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.config.JwtProperties;
import com.automotive.costing.rfq.dto.PortalDtos.CreateInviteRequest;
import com.automotive.costing.rfq.entity.SupplierMaster;
import com.automotive.costing.rfq.entity.SupplierStatus;
import com.automotive.costing.rfq.repository.SupplierPortalAccountRepository;
import com.automotive.costing.rfq.repository.SupplierPortalInviteRepository;
import com.automotive.costing.rfq.repository.SupplierPortalRfqAccessRepository;
import com.automotive.costing.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierPortalServiceTest {

    @Mock private SupplierPortalAccountRepository accountRepository;
    @Mock private SupplierPortalInviteRepository inviteRepository;
    @Mock private SupplierPortalRfqAccessRepository rfqAccessRepository;
    @Mock private RfqService rfqService;
    @Mock private SupplierMasterService supplierMasterService;
    @Mock private SupplierQuoteService supplierQuoteService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private JwtProperties jwtProperties;
    @Mock private AuditService auditService;
    @Mock private PortalEventLogRepository portalEventLogRepository;
    @Mock private NotificationService notificationService;
    @Mock private UserRepository userRepository;

    
    @Mock private TenantAccessGuard tenantAccessGuard;
    @org.junit.jupiter.api.BeforeEach
    void stubTenantAccessGuard() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
    }
@InjectMocks
    private SupplierPortalService service;

    @org.junit.jupiter.api.BeforeEach
    void notifyStubs() {
        org.mockito.Mockito.lenient().when(userRepository.findActiveByRoleCode(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.List.of());
    }

    @Test
    void createInvite_rejectsInactiveSupplier() {
        SupplierMaster s = new SupplierMaster();
        s.setSupplierId("sup-1");
        s.setStatus(SupplierStatus.INACTIVE);
        when(supplierMasterService.require("sup-1")).thenReturn(s);

        assertThatThrownBy(() -> service.createInvite(new CreateInviteRequest("sup-1", "a@b.com")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    void createInvite_returnsRawTokenOnce() {
        SupplierMaster s = new SupplierMaster();
        s.setSupplierId("sup-1");
        s.setStatus(SupplierStatus.ACTIVE);
        when(supplierMasterService.require("sup-1")).thenReturn(s);
        when(accountRepository.existsByEmailIgnoreCase("a@b.com")).thenReturn(false);
        when(inviteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.createInvite(new CreateInviteRequest("sup-1", "a@b.com"));
        assertThat(resp.inviteToken()).isNotBlank();
        assertThat(resp.email()).isEqualTo("a@b.com");
    }
}
