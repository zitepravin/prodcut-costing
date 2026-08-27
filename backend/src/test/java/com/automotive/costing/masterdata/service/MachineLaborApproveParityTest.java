package com.automotive.costing.masterdata.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.masterdata.entity.LaborMaster;
import com.automotive.costing.masterdata.entity.MachineMaster;
import com.automotive.costing.masterdata.entity.MasterDataStatus;
import com.automotive.costing.masterdata.mapper.LaborMasterMapper;
import com.automotive.costing.masterdata.mapper.LaborMasterMapperImpl;
import com.automotive.costing.masterdata.mapper.MachineMasterMapper;
import com.automotive.costing.masterdata.mapper.MachineMasterMapperImpl;
import com.automotive.costing.masterdata.repository.LaborMasterRepository;
import com.automotive.costing.masterdata.repository.MachineMasterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * V227 — Machine/Labor approve + effective-as-of parity with Material (V226).
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class MachineLaborApproveParityTest {

    @Mock MachineMasterRepository machineRepo;
    @Mock LaborMasterRepository laborRepo;
    @Mock AuditService auditService;
    @Mock TenantAccessGuard tenantAccessGuard;

    MachineMasterService machineService;
    LaborMasterService laborService;

        @BeforeEach
    void setUp() {
        when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
        MachineMasterMapper mm = new MachineMasterMapperImpl();
        LaborMasterMapper lm = new LaborMasterMapperImpl();
        com.automotive.costing.platform.user.service.AccessScopeService accessScope =
                org.mockito.Mockito.mock(com.automotive.costing.platform.user.service.AccessScopeService.class);
        machineService = new MachineMasterService(machineRepo, mm, auditService, accessScope, tenantAccessGuard);
        laborService = new LaborMasterService(laborRepo, lm, auditService, tenantAccessGuard);
    }

    @Test
    void machine_approve_closesPriorWindow() {
        MachineMaster v1 = new MachineMaster();
        v1.setRecordId("m1");
        v1.setCode("MC-01");
        v1.setRate(new BigDecimal("500"));
        v1.setVersionNumber(1);
        v1.setStatus(MasterDataStatus.ACTIVE);
        v1.setEffectiveFrom(LocalDate.of(2026, 1, 1));

        MachineMaster v2 = new MachineMaster();
        v2.setRecordId("m2");
        v2.setCode("MC-01");
        v2.setRate(new BigDecimal("550"));
        v2.setVersionNumber(2);
        v2.setStatus(MasterDataStatus.PENDING_APPROVAL);
        v2.setEffectiveFrom(LocalDate.of(2026, 4, 1));
        v2.setUom("HR");

        when(machineRepo.findByCustomerIdAndRecordId("cust-test-1", "m2")).thenReturn(Optional.of(v2));
        when(machineRepo.findByCustomerIdAndCodeOrderByVersionNumberDesc("cust-test-1", "MC-01")).thenReturn(List.of(v2, v1));
        when(machineRepo.save(any(MachineMaster.class))).thenAnswer(i -> i.getArgument(0));

        var r = machineService.approve("m2");
        assertThat(r.status()).isEqualTo(MasterDataStatus.ACTIVE);
        assertThat(v1.getEffectiveTo()).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void labor_approve_rejectsActive() {
        LaborMaster active = new LaborMaster();
        active.setRecordId("l1");
        active.setStatus(MasterDataStatus.ACTIVE);
        when(laborRepo.findByCustomerIdAndRecordId("cust-test-1", "l1")).thenReturn(Optional.of(active));
        assertThatThrownBy(() -> laborService.approve("l1"))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void labor_findEffectiveAsOf_historical() {
        LaborMaster v1 = new LaborMaster();
        v1.setRecordId("l1");
        v1.setCode("LAB-01");
        v1.setRate(new BigDecimal("200"));
        v1.setVersionNumber(1);
        v1.setStatus(MasterDataStatus.ACTIVE);
        v1.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        v1.setEffectiveTo(LocalDate.of(2026, 3, 31));
        v1.setUom("HR");
        when(laborRepo.findEffectiveAsOf("LAB-01", LocalDate.of(2026, 2, 1)))
                .thenReturn(Optional.of(v1));
        var r = laborService.findEffectiveAsOf("LAB-01", LocalDate.of(2026, 2, 1));
        assertThat(r.rate()).isEqualByComparingTo("200");
    }
}
