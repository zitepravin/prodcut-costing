package com.automotive.costing.process.service;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.bom.service.BomNodeService;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.masterdata.entity.MachineMaster;
import com.automotive.costing.masterdata.entity.MasterDataStatus;
import com.automotive.costing.masterdata.repository.LaborMasterRepository;
import com.automotive.costing.masterdata.repository.MachineMasterRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.process.dto.ProcessCostDtos.CalculateRequest;
import com.automotive.costing.process.dto.ProcessCostDtos.Response;
import com.automotive.costing.process.entity.ProcessCostCalculation;
import com.automotive.costing.process.entity.ProcessOperation;
import com.automotive.costing.process.entity.ProcessRouting;
import com.automotive.costing.process.repository.ProcessCostCalculationRepository;
import com.automotive.costing.process.repository.ProcessOperationRepository;
import com.automotive.costing.process.repository.ProcessRoutingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessCostComponentIntegrationTest {

    @Mock ProcessRoutingRepository routingRepository;
    @Mock ProcessOperationRepository operationRepository;
    @Mock ProcessCostCalculationRepository calculationRepository;
    @Mock MachineMasterRepository machineMasterRepository;
    @Mock TenantAccessGuard tenantAccessGuard;
    @Mock LaborMasterRepository laborMasterRepository;
    @Mock BomNodeRepository bomNodeRepository;
    @Mock BomNodeService bomNodeService;
    @Mock AuditService auditService;

    ProcessCostService service;
    static final LocalDate HIST = LocalDate.of(2026, 4, 1);

    @BeforeEach
    void setUp() {
        service = new ProcessCostService(
                routingRepository, operationRepository, calculationRepository,
                machineMasterRepository, tenantAccessGuard, laborMasterRepository,
                bomNodeRepository, bomNodeService, auditService);
        when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-1");
    }

    @Test
    void calculate_persistsPowerAndInspectionComponents() {
        ProcessRouting routing = new ProcessRouting();
        routing.setRoutingId("rt-1");
        routing.setBomNodeId("bom-1");
        routing.setCustomerId("cust-1");

        ProcessOperation op = new ProcessOperation();
        op.setOperationId("op-1");
        op.setRoutingId("rt-1");
        op.setSequenceNumber(10);
        op.setCycleTimeSeconds(new BigDecimal("3600")); // 1h
        op.setOperatorCount(BigDecimal.ONE);
        op.setMachineCode("MC-01");
        op.setLaborCode(null);
        op.setMachineKw(new BigDecimal("10"));
        op.setElectricityRate(new BigDecimal("2.00"));
        op.setElectricityRateCurrency("USD");
        op.setInspectionTimeHours(new BigDecimal("0.1"));
        op.setInspectorRate(new BigDecimal("50"));

        MachineMaster machine = new MachineMaster();
        machine.setCode("MC-01");
        machine.setRate(new BigDecimal("100"));
        machine.setStatus(MasterDataStatus.ACTIVE);

        BomNode bom = new BomNode();
        bom.setBomNodeId("bom-1");
        bom.setProgramId("prog-1");

        when(routingRepository.findByCustomerIdAndRoutingId("cust-1", "rt-1")).thenReturn(Optional.of(routing));
        when(operationRepository.findByRoutingIdOrderBySequenceNumberAsc("rt-1")).thenReturn(List.of(op));
        when(machineMasterRepository.findFirstByCustomerIdAndCodeAndStatusOrderByVersionNumberDesc(
                eq("cust-1"), eq("MC-01"), eq(MasterDataStatus.ACTIVE))).thenReturn(Optional.of(machine));
        when(calculationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bomNodeRepository.findByBomNodeId("bom-1")).thenReturn(Optional.of(bom));

        Response resp = service.calculate(new CalculateRequest("rt-1", HIST));

        // machine 100 + power 10*1*2=20 + inspection 0.1*50=5 = 125
        assertThat(resp.totalProcessCost()).isEqualByComparingTo("125.000000");
        assertThat(resp.machineCost()).isEqualByComparingTo("100.000000");
        assertThat(resp.powerCost()).isEqualByComparingTo("20.000000");
        assertThat(resp.inspectionCost()).isEqualByComparingTo("5.000000");
        assertThat(resp.costingDate()).isEqualTo(HIST);

        ArgumentCaptor<ProcessCostCalculation> cap = ArgumentCaptor.forClass(ProcessCostCalculation.class);
        verify(calculationRepository).save(cap.capture());
        ProcessCostCalculation saved = cap.getValue();
        assertThat(saved.getPowerCost()).isEqualByComparingTo("20.000000");
        assertThat(saved.getInspectionCost()).isEqualByComparingTo("5.000000");
        assertThat(saved.getCostingDate()).isEqualTo(HIST);
        assertThat(saved.getComponentBreakdown()).contains("POWER");

        verify(bomNodeService).triggerRollupForProgram("prog-1", HIST);
    }
}
