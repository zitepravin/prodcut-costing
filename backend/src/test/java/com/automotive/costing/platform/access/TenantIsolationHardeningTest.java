package com.automotive.costing.platform.access;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.platform.access.entity.CustomerStatus;
import com.automotive.costing.platform.access.entity.MembershipStatus;
import com.automotive.costing.platform.access.entity.PlatformCustomer;
import com.automotive.costing.platform.access.repository.CustomerModuleEntitlementRepository;
import com.automotive.costing.platform.access.repository.CustomerUserMembershipRepository;
import com.automotive.costing.platform.access.repository.PlatformCustomerRepository;
import com.automotive.costing.platform.access.repository.PlatformModuleRepository;
import com.automotive.costing.platform.access.service.CentralAccessService;
import com.automotive.costing.platform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantIsolationHardeningTest {

    @Mock PlatformCustomerRepository customerRepository;
    @Mock PlatformModuleRepository moduleRepository;
    @Mock CustomerModuleEntitlementRepository entitlementRepository;
    @Mock CustomerUserMembershipRepository membershipRepository;
    @Mock UserRepository userRepository;
    @Mock AuditService auditService;
    @InjectMocks CentralAccessService service;

    @Test
    void suspendedCustomer_blocksMembershipAccess() {
        PlatformCustomer c = new PlatformCustomer();
        c.setCustomerId("cust-a");
        c.setStatus(CustomerStatus.SUSPENDED);
        when(customerRepository.findById("cust-a")).thenReturn(Optional.of(c));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.assertUserInCustomer("cust-a", "user-1"));
        assertEquals("ACCESS-09", ex.getRuleReference());
    }

    @Test
    void activeCustomer_requiresMembership() {
        PlatformCustomer c = new PlatformCustomer();
        c.setCustomerId("cust-a");
        c.setStatus(CustomerStatus.ACTIVE);
        when(customerRepository.findById("cust-a")).thenReturn(Optional.of(c));
        when(membershipRepository.existsByCustomerIdAndUserIdAndStatus(
                "cust-a", "user-1", MembershipStatus.ACTIVE)).thenReturn(false);

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.assertUserInCustomer("cust-a", "user-1"));
        assertEquals("ACCESS-06", ex.getRuleReference());
    }

    @Test
    void activeCustomer_withMembership_allowsAccess() {
        PlatformCustomer c = new PlatformCustomer();
        c.setCustomerId("cust-a");
        c.setStatus(CustomerStatus.ACTIVE);
        when(customerRepository.findById("cust-a")).thenReturn(Optional.of(c));
        when(membershipRepository.existsByCustomerIdAndUserIdAndStatus(
                "cust-a", "user-1", MembershipStatus.ACTIVE)).thenReturn(true);

        assertDoesNotThrow(() -> service.assertUserInCustomer("cust-a", "user-1"));
    }

    @Test
    void inactiveCustomer_blocksAccess() {
        PlatformCustomer c = new PlatformCustomer();
        c.setCustomerId("cust-a");
        c.setStatus(CustomerStatus.INACTIVE);
        when(customerRepository.findById("cust-a")).thenReturn(Optional.of(c));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.assertUserInCustomer("cust-a", "user-1"));
        assertEquals("ACCESS-09", ex.getRuleReference());
    }
}
