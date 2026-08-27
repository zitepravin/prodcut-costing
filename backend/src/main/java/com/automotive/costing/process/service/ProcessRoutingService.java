package com.automotive.costing.process.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.masterdata.repository.ProcessMasterRepository;
import com.automotive.costing.process.dto.ProcessRoutingDtos.*;
import com.automotive.costing.process.entity.ProcessOperation;
import com.automotive.costing.process.entity.ProcessRouting;
import com.automotive.costing.process.entity.RoutingStatus;
import com.automotive.costing.process.repository.ProcessOperationRepository;
import com.automotive.costing.process.repository.ProcessRoutingRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * M04 Process &amp; Labor Costing - routing/operation management (PRC-01, PRC-07's
 * "more than one routing per part" support, and the SRS 4.4.5 approval gate). Cost
 * calculation itself is {@code ProcessCostService}'s job, mirroring the
 * BOM-structure-vs-roll-up-engine split M02 established between {@code BomNodeService}
 * and {@code BomRollupEngine}.
 */
@Service
@RequiredArgsConstructor
public class ProcessRoutingService {

    private static final String ENTITY_TYPE = "ProcessRouting";

    private final ProcessRoutingRepository routingRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final ProcessOperationRepository operationRepository;
    private final ProcessMasterRepository processMasterRepository;
    private final AuditService auditService;

    /** PRC-01: creates a routing with its full ordered operation list. */
    @Transactional
    public RoutingResponse createRouting(CreateRoutingRequest request) {
        ProcessRouting routing = new ProcessRouting();
        routing.setCustomerId(tenantAccessGuard.requireCustomerId());
        routing.setRoutingId(UUID.randomUUID().toString());
        routing.setBomNodeId(request.bomNodeId());
        routing.setStatus(RoutingStatus.DRAFT);
        ProcessRouting savedRouting = routingRepository.save(routing);

        List<ProcessOperation> operations = request.operations().stream()
                .map(op -> toOperationEntity(savedRouting.getRoutingId(), op))
                .toList();
        List<ProcessOperation> savedOperations = operationRepository.saveAll(operations);

        auditService.recordCreate(ENTITY_TYPE, savedRouting.getRoutingId(),
                "routing created with " + savedOperations.size() + " operation(s)");
        return toResponse(savedRouting, savedOperations);
    }

    /** PRC-01: adds a further operation to an existing (DRAFT) routing. */
    @Transactional
    public RoutingResponse addOperation(String routingId, OperationRequest request) {
        ProcessRouting routing = routingRepository.findByCustomerIdAndRoutingId(tenantAccessGuard.requireCustomerId(), routingId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProcessRouting", routingId));
        if (routing.getStatus() == RoutingStatus.APPROVED) {
            throw new BusinessRuleViolationException(
                    "Cannot add an operation to routing " + routingId + " - it is already Approved. "
                            + "Create a new routing (PRC-07: side-by-side alternates are supported) instead.",
                    "SRS-4.4.5-approved-routing-immutable");
        }

        operationRepository.save(toOperationEntity(routingId, request));
        List<ProcessOperation> operations = operationRepository.findByRoutingIdOrderBySequenceNumberAsc(routingId);
        auditService.recordUpdate(ENTITY_TYPE, routingId, null,
                "operation added; routing now has " + operations.size() + " operation(s)");
        return toResponse(routing, operations);
    }

    /**
     * SRS 4.4.5: "A process routing cannot be marked 'Approved' if any operation is
     * missing a machine or labor assignment."
     */
    @Transactional
    public RoutingResponse approve(String routingId) {
        ProcessRouting routing = routingRepository.findByCustomerIdAndRoutingId(tenantAccessGuard.requireCustomerId(), routingId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProcessRouting", routingId));
        List<ProcessOperation> operations = operationRepository.findByRoutingIdOrderBySequenceNumberAsc(routingId);

        if (operations.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "Cannot approve routing " + routingId + " - it has no operations.", "PRC-01");
        }
        for (ProcessOperation op : operations) {
            boolean missingMachine = op.getMachineCode() == null || op.getMachineCode().isBlank();
            boolean missingLabor = op.getLaborCode() == null || op.getLaborCode().isBlank();
            if (missingMachine || missingLabor) {
                throw new BusinessRuleViolationException(
                        "Cannot approve routing " + routingId + " - operation " + op.getSequenceNumber()
                                + " is missing a " + (missingMachine ? "machine" : "labor") + " assignment.",
                        "SRS-4.4.5-approval-requires-machine-and-labor");
            }
        }

        routing.setStatus(RoutingStatus.APPROVED);
        ProcessRouting saved = routingRepository.save(routing);
        auditService.recordUpdate(ENTITY_TYPE, routingId, null, "status -> APPROVED");
        return toResponse(saved, operations);
    }

    @Transactional(readOnly = true)
    public RoutingResponse getRouting(String routingId) {
        ProcessRouting routing = routingRepository.findByCustomerIdAndRoutingId(tenantAccessGuard.requireCustomerId(), routingId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProcessRouting", routingId));
        List<ProcessOperation> operations = operationRepository.findByRoutingIdOrderBySequenceNumberAsc(routingId);
        return toResponse(routing, operations);
    }

    /** PRC-07: every routing defined for a BOM node, for side-by-side comparison. */
    @Transactional(readOnly = true)
    public List<RoutingResponse> getRoutingsForBomNode(String bomNodeId) {
        return routingRepository.findByCustomerIdAndBomNodeId(tenantAccessGuard.requireCustomerId(), bomNodeId).stream()
                .map(r -> toResponse(r, operationRepository.findByRoutingIdOrderBySequenceNumberAsc(r.getRoutingId())))
                .toList();
    }

    /** PRC-03: the pre-built cycle-time formula reference for every known process. */
    @Transactional(readOnly = true)
    public List<CycleTimeTemplateResponse> listCycleTimeTemplates() {
        return processMasterRepository.findByCustomerIdOrderByProcessNameAsc(tenantAccessGuard.requireCustomerId()).stream()
                .map(p -> new CycleTimeTemplateResponse(
                        p.getProcessName(), p.getDefaultCycleTimeFormula(), p.getDefaultScrapPct()))
                .toList();
    }

    private ProcessOperation toOperationEntity(String routingId, OperationRequest request) {
        // customer_id denormalized for isolation queries; must match routing tenant
        ProcessOperation op = new ProcessOperation();
        op.setCustomerId(tenantAccessGuard.requireCustomerId());
        op.setOperationId(UUID.randomUUID().toString());
        op.setRoutingId(routingId);
        op.setSequenceNumber(request.sequenceNumber());
        op.setProcessName(request.processName());
        op.setMachineCode(request.machineCode());
        op.setLaborCode(request.laborCode());
        op.setCycleTimeSeconds(request.cycleTimeSeconds());
        op.setOperatorCount(request.operatorCount());
        op.setSetupTimeSeconds(request.setupTimeSeconds());
        op.setBatchSize(request.batchSize());
        op.setOutputPartsPerCycle(request.outputPartsPerCycle());
        op.setMachineKw(request.machineKw());
        op.setElectricityRate(request.electricityRate());
        op.setElectricityRateCurrency(request.electricityRateCurrency());
        op.setInspectionTimeHours(request.inspectionTimeHours());
        op.setInspectorRate(request.inspectorRate());
        op.setInspectionFixedCharge(request.inspectionFixedCharge());
        op.setConsumablesJson(request.consumablesJson());
        return op;
    }

    private RoutingResponse toResponse(ProcessRouting routing, List<ProcessOperation> operations) {
        List<OperationResponse> opResponses = operations.stream()
                .map(op -> new OperationResponse(op.getOperationId(), op.getSequenceNumber(), op.getProcessName(),
                        op.getMachineCode(), op.getLaborCode(), op.getCycleTimeSeconds(), op.getOperatorCount(),
                        op.getSetupTimeSeconds(), op.getBatchSize(), op.getOutputPartsPerCycle(),
                        op.getMachineKw(), op.getElectricityRate(), op.getElectricityRateCurrency(),
                        op.getInspectionTimeHours(), op.getInspectorRate(), op.getInspectionFixedCharge(),
                        op.getConsumablesJson()))
                .toList();
        return new RoutingResponse(routing.getRoutingId(), routing.getBomNodeId(), routing.getStatus(), opResponses);
    }
}
