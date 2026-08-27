package com.automotive.costing.platform.access;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.access.dto.CustomerAdminDtos.CreateDepartmentRequest;
import com.automotive.costing.platform.access.entity.CustomerDepartment;
import com.automotive.costing.platform.access.repository.CustomerDepartmentRepository;
import com.automotive.costing.platform.access.service.CustomerDepartmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerDepartmentServiceTest {

    @Mock CustomerDepartmentRepository departmentRepository;
    @Mock AuditService auditService;
    @InjectMocks CustomerDepartmentService service;

    @BeforeEach
    void ctx() {
        CustomerContext.setCustomerId("cust-a");
    }

    @AfterEach
    void clear() {
        CustomerContext.clear();
    }

    @Test
    void create_usesTrustedCustomerContext() {
        when(departmentRepository.existsByCustomerIdAndDepartmentCodeIgnoreCase("cust-a", "PURCHASE"))
                .thenReturn(false);
        when(departmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.create(new CreateDepartmentRequest("PURCHASE", "Purchase", null));
        assertEquals("cust-a", resp.customerId());
        assertEquals("PURCHASE", resp.departmentCode());
        assertEquals("ACTIVE", resp.status());
    }

    @Test
    void get_otherTenantDepartment_notFound() {
        when(departmentRepository.findByDepartmentIdAndCustomerId("dept-b", "cust-a"))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.get("dept-b"));
    }

    @Test
    void list_requiresCustomerContext() {
        CustomerContext.clear();
        assertThrows(BusinessRuleViolationException.class, () -> service.list());
    }
}
