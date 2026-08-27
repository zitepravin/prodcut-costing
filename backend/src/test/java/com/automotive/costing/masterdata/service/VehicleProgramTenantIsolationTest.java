package com.automotive.costing.masterdata.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.masterdata.entity.VehicleProgram;
import com.automotive.costing.masterdata.repository.VehicleProgramRepository;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.platform.user.service.AccessScopeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleProgramTenantIsolationTest {

    @Mock VehicleProgramRepository repository;
    @Mock AccessScopeService accessScopeService;

    private final TenantAccessGuard realGuard = new TenantAccessGuard();

    @AfterEach
    void clear() {
        CustomerContext.clear();
    }

    private VehicleProgramService serviceWithRealGuard() {
        return new VehicleProgramService(repository, accessScopeService, realGuard);
    }

    @Test
    void listAll_withoutTenantContext_throws() {
        assertThrows(BusinessRuleViolationException.class, () -> serviceWithRealGuard().listAll());
    }

    @Test
    void listAll_filtersByCustomerId() {
        CustomerContext.setCustomerId("cust-a");
        VehicleProgram a = new VehicleProgram();
        a.setProgramId("p1");
        a.setCustomerId("cust-a");
        a.setProgramCode("A1");
        a.setStatus("Active");
        when(repository.findByCustomerId("cust-a")).thenReturn(List.of(a));
        when(accessScopeService.restrictedValuesForCurrentUser(any())).thenReturn(Optional.empty());

        var list = serviceWithRealGuard().listAll();
        assertEquals(1, list.size());
        assertEquals("p1", list.get(0).programId());
    }

    @Test
    void getById_otherTenant_notFound() {
        CustomerContext.setCustomerId("cust-a");
        when(repository.findByCustomerIdAndProgramId("cust-a", "p-b")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> serviceWithRealGuard().getById("p-b"));
    }

    @Test
    void getById_sameTenant_ok() {
        CustomerContext.setCustomerId("cust-a");
        VehicleProgram a = new VehicleProgram();
        a.setProgramId("p1");
        a.setCustomerId("cust-a");
        a.setProgramCode("A1");
        a.setStatus("Active");
        when(repository.findByCustomerIdAndProgramId("cust-a", "p1")).thenReturn(Optional.of(a));

        var r = serviceWithRealGuard().getById("p1");
        assertEquals("p1", r.programId());
    }
}
