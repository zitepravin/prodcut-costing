package com.automotive.costing.process.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.masterdata.repository.ProcessMasterRepository;
import com.automotive.costing.process.dto.ProcessRoutingDtos.CreateRoutingRequest;
import com.automotive.costing.process.dto.ProcessRoutingDtos.OperationRequest;
import com.automotive.costing.process.dto.ProcessRoutingDtos.RoutingResponse;
import com.automotive.costing.process.entity.ProcessOperation;
import com.automotive.costing.process.entity.ProcessRouting;
import com.automotive.costing.process.entity.RoutingStatus;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProcessRoutingService, mapped to QA_Test_Case_Tracker.xlsx Module M04
 * (PRC-01) and the SRS 4.4.5 approval business rule ("cannot be marked 'Approved' if any
 * operation is missing a machine or labor assignment").
 */
@ExtendWith(MockitoExtension.class)
class ProcessRoutingServiceTest {

    @Mock private ProcessRoutingRepository routingRepository;
    @Mock private TenantAccessGuard tenantAccessGuard;
    @Mock private ProcessOperationRepository operationRepository;
    @Mock private ProcessMasterRepository processMasterRepository;
    @Mock private AuditService auditService;

    private ProcessRoutingService service;

    @BeforeEach
    void setUp() {
        lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-1");
        service = new ProcessRoutingService(routingRepository, tenantAccessGuard, operationRepository, processMasterRepository, auditService);
    }

    private OperationRequest fullyAssignedOperation(int seq) {
        return new OperationRequest(seq, "Stamping", "MC-001", "LB-001",
                new BigDecimal("45"), BigDecimal.ONE, null, null);
    }

    private OperationRequest missingLaborOperation(int seq) {
        return new OperationRequest(seq, "Stamping", "MC-001", null,
                new BigDecimal("45"), BigDecimal.ONE, null, null);
    }

    private ProcessOperation toEntity(OperationRequest r) {
        ProcessOperation op = new ProcessOperation();
        op.setOperationId("op-" + r.sequenceNumber());
        op.setSequenceNumber(r.sequenceNumber());
        op.setMachineCode(r.machineCode());
        op.setLaborCode(r.laborCode());
        op.setCycleTimeSeconds(r.cycleTimeSeconds());
        op.setOperatorCount(r.operatorCount());
        return op;
    }

    // TC-0049 (PRC-01, Positive): a routing with fully-assigned operations is created.
    @Test
    void createRouting_withValidOperations_persistsRoutingAndOperations() {
        when(routingRepository.save(any(ProcessRouting.class))).thenAnswer(inv -> inv.getArgument(0));
        when(operationRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateRoutingRequest request = new CreateRoutingRequest("node-1", List.of(fullyAssignedOperation(1)));
        RoutingResponse response = service.createRouting(request);

        assertThat(response.bomNodeId()).isEqualTo("node-1");
        assertThat(response.status()).isEqualTo(RoutingStatus.DRAFT);
        assertThat(response.operations()).hasSize(1);
        verify(auditService).recordCreate(eq("ProcessRouting"), anyString(), any());
    }

    // SRS 4.4.5 (Positive): approval succeeds when every operation has both assignments.
    @Test
    void approve_withAllOperationsFullyAssigned_succeeds() {
        ProcessRouting routing = new ProcessRouting();
        routing.setRoutingId("routing-1");
        routing.setBomNodeId("node-1");
        routing.setStatus(RoutingStatus.DRAFT);
        when(routingRepository.findByCustomerIdAndRoutingId("cust-1", "routing-1")).thenReturn(Optional.of(routing));
        when(operationRepository.findByRoutingIdOrderBySequenceNumberAsc("routing-1"))
                .thenReturn(List.of(toEntity(fullyAssignedOperation(1))));
        when(routingRepository.save(any(ProcessRouting.class))).thenAnswer(inv -> inv.getArgument(0));

        RoutingResponse response = service.approve("routing-1");

        assertThat(response.status()).isEqualTo(RoutingStatus.APPROVED);
    }

    // SRS 4.4.5 (Negative/Boundary): approval blocked when an operation is missing labor.
    @Test
    void approve_withOperationMissingLaborAssignment_throwsBusinessRuleViolation() {
        ProcessRouting routing = new ProcessRouting();
        routing.setRoutingId("routing-1");
        routing.setBomNodeId("node-1");
        routing.setStatus(RoutingStatus.DRAFT);
        when(routingRepository.findByCustomerIdAndRoutingId("cust-1", "routing-1")).thenReturn(Optional.of(routing));
        when(operationRepository.findByRoutingIdOrderBySequenceNumberAsc("routing-1"))
                .thenReturn(List.of(toEntity(missingLaborOperation(1))));

        assertThatThrownBy(() -> service.approve("routing-1"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("labor");

        verify(routingRepository, never()).save(any());
    }

    @Test
    void approve_withNoOperations_throwsBusinessRuleViolation() {
        ProcessRouting routing = new ProcessRouting();
        routing.setRoutingId("routing-1");
        routing.setBomNodeId("node-1");
        routing.setStatus(RoutingStatus.DRAFT);
        when(routingRepository.findByCustomerIdAndRoutingId("cust-1", "routing-1")).thenReturn(Optional.of(routing));
        when(operationRepository.findByRoutingIdOrderBySequenceNumberAsc("routing-1")).thenReturn(List.of());

        assertThatThrownBy(() -> service.approve("routing-1"))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    // SRS 4.4.5-adjacent: an Approved routing is immutable - no further operations may be added.
    @Test
    void addOperation_toApprovedRouting_throwsBusinessRuleViolation() {
        ProcessRouting routing = new ProcessRouting();
        routing.setRoutingId("routing-1");
        routing.setBomNodeId("node-1");
        routing.setStatus(RoutingStatus.APPROVED);
        when(routingRepository.findByCustomerIdAndRoutingId("cust-1", "routing-1")).thenReturn(Optional.of(routing));

        assertThatThrownBy(() -> service.addOperation("routing-1", fullyAssignedOperation(2)))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(operationRepository, never()).save(any());
    }
}
