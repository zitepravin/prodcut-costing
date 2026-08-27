package com.automotive.costing.purchase.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.purchase.entity.PurchaseRequest;
import com.automotive.costing.purchase.repository.PurchaseRequestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Verifies tenant-scoped repository contract used by PurchaseService.requirePr / list.
 */
@ExtendWith(MockitoExtension.class)
class PurchaseRequestTenantIsolationTest {

    @Mock PurchaseRequestRepository repository;
    private final TenantAccessGuard guard = new TenantAccessGuard();

    @AfterEach
    void clear() {
        CustomerContext.clear();
    }

    @Test
    void missingContext_requireCustomerId_throws() {
        assertThrows(BusinessRuleViolationException.class, guard::requireCustomerId);
    }

    @Test
    void list_scopedToCustomer() {
        CustomerContext.setCustomerId("cust-a");
        PurchaseRequest pr = new PurchaseRequest();
        pr.setPurchaseRequestId("pr-1");
        pr.setCustomerId("cust-a");
        when(repository.findByCustomerIdOrderByCreatedAtDesc("cust-a")).thenReturn(List.of(pr));
        assertEquals(1, repository.findByCustomerIdOrderByCreatedAtDesc(guard.requireCustomerId()).size());
    }

    @Test
    void crossTenant_lookupEmpty() {
        CustomerContext.setCustomerId("cust-a");
        when(repository.findByCustomerIdAndPurchaseRequestId("cust-a", "pr-b")).thenReturn(Optional.empty());
        assertTrue(repository.findByCustomerIdAndPurchaseRequestId(guard.requireCustomerId(), "pr-b").isEmpty());
    }

    @Test
    void assertOwns_rejectsOtherTenant() {
        CustomerContext.setCustomerId("cust-a");
        assertThrows(BusinessRuleViolationException.class,
                () -> guard.assertOwns("cust-b", "PurchaseRequest", "pr-1"));
    }
}
