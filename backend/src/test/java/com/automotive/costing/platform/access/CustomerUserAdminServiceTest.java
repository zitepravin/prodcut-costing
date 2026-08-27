package com.automotive.costing.platform.access;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.platform.access.dto.CustomerAdminDtos.AcceptInvitationRequest;
import com.automotive.costing.platform.access.dto.CustomerAdminDtos.CreateCustomerUserRequest;
import com.automotive.costing.platform.access.entity.UserInvitation;
import com.automotive.costing.platform.access.repository.CustomerDepartmentRepository;
import com.automotive.costing.platform.access.repository.CustomerUserMembershipRepository;
import com.automotive.costing.platform.access.repository.UserInvitationRepository;
import com.automotive.costing.platform.access.service.CentralAccessService;
import com.automotive.costing.platform.access.service.CommercialLicenseService;
import com.automotive.costing.platform.access.service.CustomerUserAdminService;
import com.automotive.costing.platform.user.entity.Role;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.repository.RoleRepository;
import com.automotive.costing.platform.user.repository.UserRepository;
import com.automotive.costing.platform.user.service.PasswordPolicyValidator;
import com.automotive.costing.platform.user.service.PermissionMatrixService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerUserAdminServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock CustomerUserMembershipRepository membershipRepository;
    @Mock CustomerDepartmentRepository departmentRepository;
    @Mock UserInvitationRepository invitationRepository;
    @Mock CentralAccessService centralAccessService;
    @Mock CommercialLicenseService commercialLicenseService;
    @Mock PermissionMatrixService permissionMatrixService;
    @Mock PasswordPolicyValidator passwordPolicyValidator;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuditService auditService;
    @Mock ObjectProvider<JavaMailSender> mailSenderProvider;

    @InjectMocks CustomerUserAdminService service;

    @BeforeEach
    void setCustomer() {
        CustomerContext.setCustomerId("cust-a");
    }

    @AfterEach
    void clear() {
        CustomerContext.clear();
    }

    @Test
    void createUser_rejectsPlatformOwnerRole() {
        when(permissionMatrixService.isAssignableByCustomerAdmin("PLATFORM_OWNER")).thenReturn(false);
        CreateCustomerUserRequest req = new CreateCustomerUserRequest(
                "Rahul", "rahul@a.com", null, null, null, null, Set.of("PLATFORM_OWNER"));
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.createUser(req));
        assertEquals("ROLE-02", ex.getRuleReference());
    }

    @Test
    void createUser_rejectsSystemAdministrator() {
        when(permissionMatrixService.isAssignableByCustomerAdmin("SYSTEM_ADMINISTRATOR")).thenReturn(false);
        CreateCustomerUserRequest req = new CreateCustomerUserRequest(
                "Rahul", "rahul@a.com", null, null, null, null, Set.of("SYSTEM_ADMINISTRATOR"));
        assertThrows(BusinessRuleViolationException.class, () -> service.createUser(req));
    }

    @Test
    void createUser_succeedsForBuyer_andCreatesInvitation() {
        when(permissionMatrixService.isAssignableByCustomerAdmin("PURCHASING_BUYER")).thenReturn(true);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        Role role = new Role();
        role.setRoleId("r1");
        role.setRoleCode("PURCHASING_BUYER");
        when(roleRepository.findByRoleCode("PURCHASING_BUYER")).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(invitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        var result = service.createUser(new CreateCustomerUserRequest(
                "Rahul Sharma", "rahul@customer.com", "EMP-1", null, "Buyer", null,
                Set.of("PURCHASING_BUYER")));

        assertTrue(result.invitationCreated());
        assertEquals("Invited", result.user().status());
        assertNotNull(result.invitationDeliveryNote());
        verify(invitationRepository).save(any(UserInvitation.class));
        verify(membershipRepository).save(any());
    }

    @Test
    void getUser_otherTenant_forbidden() {
        when(membershipRepository.findByCustomerIdAndUserId("cust-a", "user-b")).thenReturn(Optional.empty());
        when(membershipRepository.existsByCustomerIdAndUserIdAndStatus(eq("cust-a"), eq("user-b"), any()))
                .thenReturn(false);
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.getUser("user-b"));
        assertEquals("ACCESS-06", ex.getRuleReference());
    }

    @Test
    void acceptInvitation_expired_rejected() {
        UserInvitation inv = new UserInvitation();
        inv.setInvitationId("i1");
        inv.setStatus("PENDING");
        inv.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        inv.setUserId("u1");
        inv.setCustomerId("cust-a");
        when(invitationRepository.findByTokenHashAndStatus(anyString(), eq("PENDING")))
                .thenReturn(Optional.of(inv));
        when(invitationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.acceptInvitation(new AcceptInvitationRequest("sometoken", "Password1!")));
        assertEquals("INVITE-02", ex.getRuleReference());
        assertEquals("EXPIRED", inv.getStatus());
    }

    @Test
    void acceptInvitation_reuse_rejected() {
        when(invitationRepository.findByTokenHashAndStatus(anyString(), eq("PENDING")))
                .thenReturn(Optional.empty());
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.acceptInvitation(new AcceptInvitationRequest("used", "Password1!")));
        assertEquals("INVITE-01", ex.getRuleReference());
    }

    @Test
    void listUsers_requiresCustomerContext() {
        CustomerContext.clear();
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.listUsers());
        assertEquals("ACCESS-10", ex.getRuleReference());
    }
}
