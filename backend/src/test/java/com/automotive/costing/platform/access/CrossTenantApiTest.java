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

/**
 * V224 — Cross-tenant API isolation.
 * Customer A membership must not grant access to Customer B context.
 * Manipulating customerId alone must fail authorization.
 */
@ExtendWith(MockitoExtension.class)
class CrossTenantApiTest {

    @Mock PlatformCustomerRepository customerRepository;
    @Mock PlatformModuleRepository moduleRepository;
    @Mock CustomerModuleEntitlementRepository entitlementRepository;
    @Mock CustomerUserMembershipRepository membershipRepository;
    @Mock UserRepository userRepository;
    @Mock AuditService auditService;
    @InjectMocks CentralAccessService service;

    @Test
    void userOfCustomerA_cannotEnterCustomerB() {
        PlatformCustomer b = new PlatformCustomer();
        b.setCustomerId("customer-b");
        b.setStatus(CustomerStatus.ACTIVE);
        when(customerRepository.findById("customer-b")).thenReturn(Optional.of(b));
        when(membershipRepository.existsByCustomerIdAndUserIdAndStatus(
                "customer-b", "user-a", MembershipStatus.ACTIVE)).thenReturn(false);

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.assertUserInCustomer("customer-b", "user-a"));
        assertEquals("ACCESS-06", ex.getRuleReference());
    }

    @Test
    void userOfCustomerA_canEnterOwnCustomer() {
        PlatformCustomer a = new PlatformCustomer();
        a.setCustomerId("customer-a");
        a.setStatus(CustomerStatus.ACTIVE);
        when(customerRepository.findById("customer-a")).thenReturn(Optional.of(a));
        when(membershipRepository.existsByCustomerIdAndUserIdAndStatus(
                "customer-a", "user-a", MembershipStatus.ACTIVE)).thenReturn(true);

        assertDoesNotThrow(() -> service.assertUserInCustomer("customer-a", "user-a"));
    }

    @Test
    void suspendedCustomerB_blocksEvenIfMembershipRowExists() {
        PlatformCustomer b = new PlatformCustomer();
        b.setCustomerId("customer-b");
        b.setStatus(CustomerStatus.SUSPENDED);
        when(customerRepository.findById("customer-b")).thenReturn(Optional.of(b));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.assertUserInCustomer("customer-b", "user-a"));
        assertEquals("ACCESS-09", ex.getRuleReference());
    }

    @Test
    void hasActiveMembership_false_forForeignTenant() {
        when(membershipRepository.existsByCustomerIdAndUserIdAndStatus(
                "customer-b", "user-a", MembershipStatus.ACTIVE)).thenReturn(false);
        assertFalse(service.hasActiveMembership("customer-b", "user-a"));
    }

    @Test
    void hasActiveMembership_true_forOwnTenant() {
        when(membershipRepository.existsByCustomerIdAndUserIdAndStatus(
                "customer-a", "user-a", MembershipStatus.ACTIVE)).thenReturn(true);
        assertTrue(service.hasActiveMembership("customer-a", "user-a"));
    }
}
