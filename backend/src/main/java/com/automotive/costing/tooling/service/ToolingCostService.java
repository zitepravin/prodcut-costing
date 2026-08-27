package com.automotive.costing.tooling.service;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.bom.service.BomNodeService;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.masterdata.entity.VehicleProgram;
import com.automotive.costing.masterdata.repository.VehicleProgramRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.tooling.dto.ToolingCostDtos.*;
import com.automotive.costing.tooling.engine.ToolingCostEngine;
import com.automotive.costing.tooling.engine.ToolingCostEngine.ToolingCostInput;
import com.automotive.costing.tooling.engine.ToolingCostEngine.ToolingCostResult;
import com.automotive.costing.tooling.entity.ToolingCostCalculation;
import com.automotive.costing.tooling.entity.ToolingRecord;
import com.automotive.costing.tooling.repository.ToolingCostCalculationRepository;
import com.automotive.costing.tooling.repository.ToolingRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * M06 Tooling, Die &amp; Fixture Investment Costing - cost calculation (TL-02, TL-04,
 * TL-05, TL-06). Resolves the owning {@code VehicleProgram}'s lifetime volume at
 * calculation time (never cached on the tooling record), delegates arithmetic entirely to
 * {@link ToolingCostEngine}, persists the result, then rolls up the owning BOM node's
 * program - the same "calculate -&gt; persist -&gt; roll up immediately" shape M03/M04
 * established.
 */
@Service
@RequiredArgsConstructor
public class ToolingCostService {

    private static final String ENTITY_TYPE = "ToolingCostCalculation";
    private static final int TL06_MAX_COMPARISON_SIZE = 5;

    private final ToolingRecordRepository toolingRecordRepository;
    private final ToolingCostCalculationRepository calculationRepository;
    private final BomNodeRepository bomNodeRepository;
    private final VehicleProgramRepository vehicleProgramRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final BomNodeService bomNodeService;
    private final AuditService auditService;

    /** TL-02/04: calculates, persists, and rolls up the owning BOM node's program. */
    @Transactional
    public Response calculate(CalculateRequest request) {
        ToolingRecord toolingRecord = toolingRecordRepository.findByToolingRecordId(request.toolingRecordId())
                .orElseThrow(() -> ResourceNotFoundException.of("ToolingRecord", request.toolingRecordId()));

        BomNode bomNode = bomNodeRepository.findByBomNodeId(toolingRecord.getBomNodeId())
                .orElseThrow(() -> ResourceNotFoundException.of("BomNode", toolingRecord.getBomNodeId()));

        VehicleProgram program = vehicleProgramRepository.findByCustomerIdAndProgramId(tenantAccessGuard.requireCustomerId(), bomNode.getProgramId())
                .orElseThrow(() -> ResourceNotFoundException.of("VehicleProgram", bomNode.getProgramId()));

        ToolingCostResult result = ToolingCostEngine.calculate(new ToolingCostInput(
                toolingRecord.getToolCost(), program.getLifetimeVolume(), toolingRecord.getToolLifeHits(),
                toolingRecord.getRefurbishmentCostPerEvent(), toolingRecord.getOwnership()));

        ToolingCostCalculation entity = new ToolingCostCalculation();
        entity.setCalculationId(UUID.randomUUID().toString());
        entity.setToolingRecordId(toolingRecord.getToolingRecordId());
        entity.setBomNodeId(toolingRecord.getBomNodeId());
        entity.setProgramId(program.getProgramId());
        entity.setBaseAmortizedCostPerPiece(result.baseAmortizedCostPerPiece());
        entity.setRefurbishmentEventsProjected(result.refurbishmentEventsProjected());
        entity.setRefurbishmentCostPerPiece(result.refurbishmentCostPerPiece());
        entity.setTotalAmortizedCostPerPiece(result.totalAmortizedCostPerPiece());
        entity.setIncludedInPiecePrice(result.includedInPiecePrice());
        entity.setCalculationBasis(result.calculationBasis());
        entity.setCalculatedAt(Instant.now());

        ToolingCostCalculation saved = calculationRepository.save(entity);
        auditService.recordCreate(ENTITY_TYPE, saved.getCalculationId(), toResponse(saved));

        bomNodeService.triggerRollupForProgram(program.getProgramId());

        return toResponse(saved);
    }

    /** TL-06: compare tooling cost across 2-5 alternate supplier proposals for the same part. */
    @Transactional(readOnly = true)
    public CompareResponse compare(CompareRequest request) {
        if (request.toolingRecordIds() == null || request.toolingRecordIds().size() < 2) {
            throw new BusinessRuleViolationException(
                    "At least two tooling records are required to run a comparison.", "TL-06");
        }
        if (request.toolingRecordIds().size() > TL06_MAX_COMPARISON_SIZE) {
            throw new BusinessRuleViolationException(
                    "TL-06 supports comparing up to " + TL06_MAX_COMPARISON_SIZE + " tooling records at a time.",
                    "TL-06");
        }

        List<CompareLine> lines = request.toolingRecordIds().stream()
                .map(this::compareOne)
                .sorted(Comparator.comparing(CompareLine::totalAmortizedCostPerPiece))
                .toList();

        String lowest = lines.isEmpty() ? null : lines.get(0).toolingRecordId();
        return new CompareResponse(lines, lowest);
    }

    private CompareLine compareOne(String toolingRecordId) {
        ToolingRecord toolingRecord = toolingRecordRepository.findByToolingRecordId(toolingRecordId)
                .orElseThrow(() -> ResourceNotFoundException.of("ToolingRecord", toolingRecordId));
        BomNode bomNode = bomNodeRepository.findByBomNodeId(toolingRecord.getBomNodeId())
                .orElseThrow(() -> ResourceNotFoundException.of("BomNode", toolingRecord.getBomNodeId()));
        VehicleProgram program = vehicleProgramRepository.findByCustomerIdAndProgramId(tenantAccessGuard.requireCustomerId(), bomNode.getProgramId())
                .orElseThrow(() -> ResourceNotFoundException.of("VehicleProgram", bomNode.getProgramId()));

        ToolingCostResult result = ToolingCostEngine.calculate(new ToolingCostInput(
                toolingRecord.getToolCost(), program.getLifetimeVolume(), toolingRecord.getToolLifeHits(),
                toolingRecord.getRefurbishmentCostPerEvent(), toolingRecord.getOwnership()));

        return new CompareLine(toolingRecordId, toolingRecord.getSupplierId(),
                result.totalAmortizedCostPerPiece(), result.includedInPiecePrice());
    }

    private Response toResponse(ToolingCostCalculation c) {
        return new Response(c.getCalculationId(), c.getToolingRecordId(), c.getBomNodeId(), c.getProgramId(),
                c.getBaseAmortizedCostPerPiece(), c.getRefurbishmentEventsProjected(),
                c.getRefurbishmentCostPerPiece(), c.getTotalAmortizedCostPerPiece(), c.isIncludedInPiecePrice(),
                c.getCalculationBasis(), c.getCalculatedAt());
    }
}
