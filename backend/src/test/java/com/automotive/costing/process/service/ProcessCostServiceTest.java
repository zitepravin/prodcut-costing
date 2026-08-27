package com.automotive.costing.process.service;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.bom.service.BomNodeService;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.masterdata.entity.LaborMaster;
import com.automotive.costing.masterdata.entity.MachineMaster;
import com.automotive.costing.masterdata.entity.MasterDataStatus;
import com.automotive.costing.masterdata.repository.LaborMasterRepository;
import com.automotive.costing.masterdata.repository.MachineMasterRepository;
import com.automotive.costing.process.dto.ProcessCostDtos.CalculateRequest;
import com.automotive.costing.process.dto.ProcessCostDtos.CompareRequest;
import com.automotive.costing.process.dto.ProcessCostDtos.Response;
import com.automotive.costing.process.entity.ProcessCostCalculation;
import com.automotive.costing.process.entity.ProcessOperation;
import com.automotive.costing.process.entity.ProcessRouting;
import com.automotive.costing.process.entity.RoutingStatus;
import com.automotive.costing.process.repository.ProcessCostCalculationRepository;
import com.automotive.costing.process.repository.ProcessOperationRepository;
import com.automotive.costing.process.repository.ProcessRoutingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessCostServiceTest {

    @Mock private ProcessRoutingRepository routingRepository;
    @Mock private ProcessOperationRepository operationRepository;
    @Mock private ProcessCostCalculationRepository calculationRepository;
    @Mock private MachineMasterRepository machineMasterRepository;
    @Mock private TenantAccessGuard tenantAccessGuard;
    @Mock private LaborMasterRepository laborMasterRepository;
    @Mock private BomNodeRepository bomNodeRepository;
    @Mock private BomNodeService bomNodeService;
    @Mock private AuditService auditService;

    private ProcessCostService service;

    @BeforeEach
    void setUp() {
        service = new ProcessCostService(routingRepository, operationRepository, calculationRepository, machineMasterRepository, tenantAccessGuard, laborMasterRepository, bomNodeRepository, bomNodeService, auditService);
        lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-1");
    }

    private ProcessRouting routing() {
        ProcessRouting r = new ProcessRouting();
        r.setRoutingId("routing-1");
        r.setBomNodeId("node-1");
        r.setStatus(RoutingStatus.DRAFT);
        return r;
    }

    private ProcessOperation operation() {
        ProcessOperation op = new ProcessOperation();
        op.setOperationId("op-1");
        op.setRoutingId("routing-1");
        op.setSequenceNumber(1);
        op.setMachineCode("MC-001");
        op.setLaborCode("LB-001");
        op.setCycleTimeSeconds(new BigDecimal("45"));
        op.setOperatorCount(BigDecimal.ONE);
        return op;
    }

    // TC-0055-ish (PRC-02/06, Positive): calculating a routing resolves live rates,
    // persists the result, and triggers a program roll-up (mirroring MAT-05's pattern).
    @Test
    void calculate_withValidRouting_resolvesRatesPersistsAndTriggersRollup() {
        when(routingRepository.findByCustomerIdAndRoutingId("cust-1", "routing-1")).thenReturn(Optional.of(routing()));
        when(operationRepository.findByRoutingIdOrderBySequenceNumberAsc("routing-1"))
                .thenReturn(List.of(operation()));

        MachineMaster machine = new MachineMaster();
        machine.setRate(new BigDecimal("60.000000"));
        when(machineMasterRepository.findFirstByCustomerIdAndCodeAndStatusOrderByVersionNumberDesc("cust-1", "MC-001", MasterDataStatus.ACTIVE))
                .thenReturn(Optional.of(machine));

        LaborMaster labor = new LaborMaster();
        labor.setRate(new BigDecimal("25.000000"));
        when(laborMasterRepository.findFirstByCustomerIdAndCodeAndStatusOrderByVersionNumberDesc("cust-1", "LB-001", MasterDataStatus.ACTIVE))
                .thenReturn(Optional.of(labor));

        when(calculationRepository.save(any(ProcessCostCalculation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        BomNode bomNode = new BomNode();
        bomNode.setBomNodeId("node-1");
        bomNode.setProgramId("program-1");
        when(bomNodeRepository.findByBomNodeId("node-1")).thenReturn(Optional.of(bomNode));

        Response result = service.calculate(new CalculateRequest("routing-1"));

        assertThat(result.totalProcessCost()).isEqualByComparingTo("1.062500");
        verify(bomNodeService).triggerRollupForProgram(eq("program-1"), any());
        verify(auditService).recordCreate(eq("ProcessCostCalculation"), anyString(), any());
    }

    @Test
    void calculate_withNoActiveMachineRateVersion_throwsBusinessRuleViolation() {
        when(routingRepository.findByCustomerIdAndRoutingId("cust-1", "routing-1")).thenReturn(Optional.of(routing()));
        when(operationRepository.findByRoutingIdOrderBySequenceNumberAsc("routing-1"))
                .thenReturn(List.of(operation()));
        when(machineMasterRepository.findFirstByCustomerIdAndCodeAndStatusOrderByVersionNumberDesc("cust-1", "MC-001", MasterDataStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculate(new CalculateRequest("routing-1")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("SRS 4.5.5");
    }

    // PRC-07 (Negative/Boundary): comparison requires at least two routing IDs.
    @Test
    void compareRoutings_withFewerThanTwoRoutingIds_throwsBusinessRuleViolation() {
        CompareRequest request = new CompareRequest(List.of("routing-1"));

        assertThatThrownBy(() -> service.compareRoutings(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("PRC-07");
    }
}
