package com.automotive.costing.rfq.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.rfq.entity.SupplierMaster;
import com.automotive.costing.rfq.entity.SupplierStatus;
import com.automotive.costing.rfq.repository.SupplierMasterRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierMasterTenantIsolationTest {

    @Mock SupplierMasterRepository repository;
    @Mock AuditService auditService;
    private final TenantAccessGuard guard = new TenantAccessGuard();

    @AfterEach
    void clear() {
        CustomerContext.clear();
    }

    private SupplierMasterService svc() {
        return new SupplierMasterService(repository, auditService, guard);
    }

    @Test
    void listActive_requiresTenant() {
        assertThrows(BusinessRuleViolationException.class, () -> svc().listActive());
    }

    @Test
    void listActive_scopedToCustomer() {
        CustomerContext.setCustomerId("cust-a");
        SupplierMaster s = new SupplierMaster();
        s.setSupplierId("s1");
        s.setCustomerId("cust-a");
        s.setSupplierCode("SUP1");
        s.setSupplierName("A");
        s.setCurrencyCode("USD");
        s.setStatus(SupplierStatus.ACTIVE);
        when(repository.findByCustomerIdAndStatusOrderBySupplierCodeAsc("cust-a", SupplierStatus.ACTIVE))
                .thenReturn(List.of(s));
        assertEquals(1, svc().listActive().size());
    }

    @Test
    void require_crossTenant_notFound() {
        CustomerContext.setCustomerId("cust-a");
        when(repository.findByCustomerIdAndSupplierId("cust-a", "s-b")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> svc().require("s-b"));
    }
}
