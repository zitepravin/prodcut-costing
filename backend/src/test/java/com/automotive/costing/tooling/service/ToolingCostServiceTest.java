package com.automotive.costing.tooling.service;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.bom.service.BomNodeService;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.masterdata.entity.VehicleProgram;
import com.automotive.costing.masterdata.repository.VehicleProgramRepository;
import com.automotive.costing.tooling.dto.ToolingCostDtos.CalculateRequest;
import com.automotive.costing.tooling.dto.ToolingCostDtos.CompareRequest;
import com.automotive.costing.tooling.dto.ToolingCostDtos.Response;
import com.automotive.costing.tooling.entity.ToolingCostCalculation;
import com.automotive.costing.tooling.entity.ToolingOwnership;
import com.automotive.costing.tooling.entity.ToolingRecord;
import com.automotive.costing.tooling.entity.ToolingType;
import com.automotive.costing.tooling.repository.ToolingCostCalculationRepository;
import com.automotive.costing.tooling.repository.ToolingRecordRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolingCostServiceTest {

    @Mock private ToolingRecordRepository toolingRecordRepository;
    @Mock private TenantAccessGuard tenantAccessGuard;
    @Mock private ToolingCostCalculationRepository calculationRepository;
    @Mock private BomNodeRepository bomNodeRepository;
    @Mock private VehicleProgramRepository vehicleProgramRepository;
    @Mock private BomNodeService bomNodeService;
    @Mock private AuditService auditService;

    private ToolingCostService service;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
        service = new ToolingCostService(toolingRecordRepository, calculationRepository, bomNodeRepository, vehicleProgramRepository, tenantAccessGuard, bomNodeService, auditService);
    }

    private ToolingRecord toolingRecord() {
        ToolingRecord r = new ToolingRecord();
        r.setToolingRecordId("tool-1");
        r.setBomNodeId("node-1");
        r.setToolingType(ToolingType.INJECTION_MOLD);
        r.setToolCost(new BigDecimal("500000.000000"));
        r.setOwnership(ToolingOwnership.SUPPLIER);
        return r;
    }

    private BomNode bomNode() {
        BomNode node = new BomNode();
        node.setBomNodeId("node-1");
        node.setProgramId("program-1");
        return node;
    }

    private VehicleProgram program() {
        VehicleProgram p = new VehicleProgram();
        p.setProgramId("program-1");
        p.setLifetimeVolume(new BigDecimal("100000"));
        return p;
    }

    // TC-0079-ish (TL-02, Positive): calculating amortizes the tool cost and triggers a program roll-up.
    @Test
    void calculate_withValidToolingRecordAndProgramVolume_persistsAndTriggersRollup() {
        when(toolingRecordRepository.findByToolingRecordId("tool-1")).thenReturn(Optional.of(toolingRecord()));
        when(bomNodeRepository.findByBomNodeId("node-1")).thenReturn(Optional.of(bomNode()));
        when(vehicleProgramRepository.findByCustomerIdAndProgramId("cust-test-1", "program-1")).thenReturn(Optional.of(program()));
        when(calculationRepository.save(any(ToolingCostCalculation.class))).thenAnswer(inv -> inv.getArgument(0));

        Response result = service.calculate(new CalculateRequest("tool-1"));

        assertThat(result.totalAmortizedCostPerPiece()).isEqualByComparingTo("5.000000");
        assertThat(result.includedInPiecePrice()).isTrue();
        verify(bomNodeService).triggerRollupForProgram("program-1");
        verify(auditService).recordCreate(eq("ToolingCostCalculation"), anyString(), any());
    }

    // TC-0080 (TL-02, Negative/Boundary): the program has no lifetime volume defined -> blocked, not silently zeroed.
    @Test
    void calculate_whenProgramHasNoLifetimeVolume_throwsBusinessRuleViolation() {
        when(toolingRecordRepository.findByToolingRecordId("tool-1")).thenReturn(Optional.of(toolingRecord()));
        when(bomNodeRepository.findByBomNodeId("node-1")).thenReturn(Optional.of(bomNode()));
        VehicleProgram programWithoutVolume = new VehicleProgram();
        programWithoutVolume.setProgramId("program-1");
        when(vehicleProgramRepository.findByCustomerIdAndProgramId("cust-test-1", "program-1")).thenReturn(Optional.of(programWithoutVolume));

        assertThatThrownBy(() -> service.calculate(new CalculateRequest("tool-1")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("lifetime volume");

        verifyNoInteractions(calculationRepository);
        verifyNoInteractions(bomNodeService);
    }

    // TL-06 (Negative/Boundary): fewer than two records cannot be compared.
    @Test
    void compare_withFewerThanTwoToolingRecordIds_throwsBusinessRuleViolation() {
        CompareRequest request = new CompareRequest(List.of("tool-1"));

        assertThatThrownBy(() -> service.compare(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("TL-06");
    }

    // TL-06: "up to five supplier quotes" - a sixth is rejected, not silently truncated.
    @Test
    void compare_withMoreThanFiveToolingRecordIds_throwsBusinessRuleViolation() {
        CompareRequest request = new CompareRequest(
                List.of("t1", "t2", "t3", "t4", "t5", "t6"));

        assertThatThrownBy(() -> service.compare(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("TL-06");
    }
}
