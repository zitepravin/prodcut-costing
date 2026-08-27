package com.automotive.costing.targetcosting.service;
import java.util.Map;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.versioning.entity.ChangeTrigger;
import com.automotive.costing.versioning.service.CostVersionService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.masterdata.entity.VehicleProgram;
import com.automotive.costing.masterdata.repository.VehicleProgramRepository;
import com.automotive.costing.platform.user.entity.ScopeType;
import com.automotive.costing.platform.user.service.AccessScopeService;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import com.automotive.costing.targetcosting.dto.TargetCostingDtos.*;
import com.automotive.costing.targetcosting.engine.TargetCostVarianceEngine;
import com.automotive.costing.targetcosting.engine.TargetCostVarianceEngine.VarianceInput;
import com.automotive.costing.targetcosting.engine.TargetCostVarianceEngine.VarianceResult;
import com.automotive.costing.targetcosting.entity.*;
import com.automotive.costing.targetcosting.repository.TargetCostRecordRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.targetcosting.repository.TargetCostVarianceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Target Costing: part-level targets, variance vs should-cost or BOM roll-up,
 * and approval-status lifecycle for workflow integration (entityType = TargetCostRecord).
 */
@Service
@RequiredArgsConstructor
public class TargetCostingService {

    private static final String ENTITY_TYPE = "TargetCostRecord";
    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("0.05");

    private final TargetCostRecordRepository targetRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final TargetCostVarianceRepository varianceRepository;
    private final BomNodeRepository bomNodeRepository;
    private final VehicleProgramRepository vehicleProgramRepository;
    private final ShouldCostBuildUpRepository shouldCostBuildUpRepository;
    private final AccessScopeService accessScopeService;
    private final AuditService auditService;
    private final CostVersionService costVersionService;

    @Transactional
    public TargetResponse create(CreateTargetRequest request) {
        BomNode node = bomNodeRepository.findByCustomerIdAndBomNodeId(tenantAccessGuard.requireCustomerId(), request.bomNodeId())
                .orElseThrow(() -> ResourceNotFoundException.of("BomNode", request.bomNodeId()));
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, node.getProgramId());

        VehicleProgram program = vehicleProgramRepository.findByCustomerIdAndProgramId(tenantAccessGuard.requireCustomerId(), node.getProgramId())
                .orElseThrow(() -> ResourceNotFoundException.of("VehicleProgram", node.getProgramId()));

        String currency = request.currencyCode() != null && !request.currencyCode().isBlank()
                ? request.currencyCode().trim().toUpperCase()
                : (program.getTargetCurrency() != null ? program.getTargetCurrency() : "USD");

        TargetCostRecord record = new TargetCostRecord();
        record.setCustomerId(tenantAccessGuard.requireCustomerId());
        record.setTargetCostId(UUID.randomUUID().toString());
        record.setBomNodeId(node.getBomNodeId());
        record.setProgramId(node.getProgramId());
        record.setTargetAmount(request.targetAmount());
        record.setCurrencyCode(currency);
        record.setEffectiveFrom(request.effectiveFrom() != null ? request.effectiveFrom() : LocalDate.now());
        record.setEffectiveTo(request.effectiveTo());
        record.setStatus(TargetCostStatus.DRAFT);
        record.setApprovalStatus(TargetApprovalStatus.DRAFT);
        record.setNotes(request.notes());

        TargetCostRecord saved = targetRepository.save(record);
        auditService.recordCreate(ENTITY_TYPE, saved.getTargetCostId(), toTargetResponse(saved));
        return toTargetResponse(saved);
    }

    @Transactional
    public TargetResponse update(String targetCostId, UpdateTargetRequest request) {
        TargetCostRecord record = getTargetOrThrow(targetCostId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, record.getProgramId());
        if (record.getStatus() == TargetCostStatus.SUPERSEDED) {
            throw new BusinessRuleViolationException("Cannot update a SUPERSEDED target cost", "TC-04");
        }
        if (record.getApprovalStatus() == TargetApprovalStatus.SUBMITTED) {
            throw new BusinessRuleViolationException(
                    "Cannot update a target while approval is SUBMITTED", "TC-05");
        }
        TargetResponse before = toTargetResponse(record);

        if (request.targetAmount() != null) {
            record.setTargetAmount(request.targetAmount());
        }
        if (request.currencyCode() != null && !request.currencyCode().isBlank()) {
            record.setCurrencyCode(request.currencyCode().trim().toUpperCase());
        }
        if (request.effectiveFrom() != null) {
            record.setEffectiveFrom(request.effectiveFrom());
        }
        if (request.effectiveTo() != null) {
            record.setEffectiveTo(request.effectiveTo());
        }
        if (request.notes() != null) {
            record.setNotes(request.notes());
        }
        // Edits after APPROVED revert approval to DRAFT so re-submission is required
        if (record.getApprovalStatus() == TargetApprovalStatus.APPROVED) {
            record.setApprovalStatus(TargetApprovalStatus.DRAFT);
        }

        TargetCostRecord saved = targetRepository.save(record);
        auditService.recordUpdate(ENTITY_TYPE, saved.getTargetCostId(), before, toTargetResponse(saved));
        return toTargetResponse(saved);
    }

    /**
     * Activates a target for a BOM node: supersedes any prior ACTIVE target on the same node.
     */
    @Transactional
    public TargetResponse activate(String targetCostId) {
        TargetCostRecord record = getTargetOrThrow(targetCostId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, record.getProgramId());

        targetRepository.findFirstByBomNodeIdAndStatusOrderByEffectiveFromDesc(
                        record.getBomNodeId(), TargetCostStatus.ACTIVE)
                .ifPresent(existing -> {
                    if (!existing.getTargetCostId().equals(record.getTargetCostId())) {
                        existing.setStatus(TargetCostStatus.SUPERSEDED);
                        targetRepository.save(existing);
                    }
                });

        record.setStatus(TargetCostStatus.ACTIVE);
        TargetCostRecord saved = targetRepository.save(record);
        costVersionService.snapshotAfterChange(
                CostVersionService.TYPE_TARGET_COST,
                saved.getTargetCostId(),
                ChangeTrigger.APPROVAL_BASELINE,
                saved.getTargetCostId(),
                "Target cost activated",
                java.util.Map.of(
                        "targetCostId", saved.getTargetCostId(),
                        "status", String.valueOf(saved.getStatus()),
                        "targetAmount", saved.getTargetAmount() != null ? saved.getTargetAmount().toPlainString() : ""
                ));
        auditService.recordUpdate(ENTITY_TYPE, saved.getTargetCostId(), null, toTargetResponse(saved));
        return toTargetResponse(saved);
    }

    /**
     * Marks approval status SUBMITTED — call after workflow POST /workflow/submit
     * with entityType=TargetCostRecord. Workflow listeners can complete the cycle.
     */
    @Transactional
    public TargetResponse markSubmitted(String targetCostId) {
        TargetCostRecord record = getTargetOrThrow(targetCostId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, record.getProgramId());
        if (record.getApprovalStatus() == TargetApprovalStatus.SUBMITTED) {
            return toTargetResponse(record);
        }
        record.setApprovalStatus(TargetApprovalStatus.SUBMITTED);
        return toTargetResponse(targetRepository.save(record));
    }

    @Transactional
    public TargetResponse applyApprovalOutcome(String targetCostId, boolean approved) {
        TargetCostRecord record = getTargetOrThrow(targetCostId);
        record.setApprovalStatus(approved ? TargetApprovalStatus.APPROVED : TargetApprovalStatus.REJECTED);
        if (!approved) {
            // Rejected returns to draft for rework (same pattern as Should-Cost)
            record.setApprovalStatus(TargetApprovalStatus.DRAFT);
            record.setStatus(TargetCostStatus.DRAFT);
        }
        return toTargetResponse(targetRepository.save(record));
    }

    @Transactional
    public VarianceResponse computeVariance(ComputeVarianceRequest request) {
        TargetCostRecord target = getTargetOrThrow(request.targetCostId());
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, target.getProgramId());

        BigDecimal actual;
        String actualSourceId;
        ActualCostSource source = request.actualSource();

        if (source == ActualCostSource.SHOULD_COST) {
            if (request.shouldCostBuildUpId() == null || request.shouldCostBuildUpId().isBlank()) {
                throw new BusinessRuleViolationException(
                        "shouldCostBuildUpId is required when actualSource is SHOULD_COST", "TC-06");
            }
            ShouldCostBuildUp buildUp = shouldCostBuildUpRepository.findByCustomerIdAndBuildUpId(tenantAccessGuard.requireCustomerId(), request.shouldCostBuildUpId())
                    .orElseThrow(() -> ResourceNotFoundException.of(
                            "ShouldCostBuildUp", request.shouldCostBuildUpId()));
            if (!buildUp.getBomNodeId().equals(target.getBomNodeId())) {
                throw new BusinessRuleViolationException(
                        "Should-cost build-up does not belong to the same BOM node as the target", "TC-07");
            }
            actual = buildUp.getTotalCost();
            actualSourceId = buildUp.getBuildUpId();
        } else {
            BomNode node = bomNodeRepository.findByCustomerIdAndBomNodeId(tenantAccessGuard.requireCustomerId(), target.getBomNodeId())
                    .orElseThrow(() -> ResourceNotFoundException.of("BomNode", target.getBomNodeId()));
            if (node.getRolledUpCost() == null) {
                throw new BusinessRuleViolationException(
                        "BOM node has no rolled-up cost; run roll-up before variance", "TC-08");
            }
            actual = node.getRolledUpCost();
            actualSourceId = node.getBomNodeId();
        }

        BigDecimal threshold = request.thresholdPct() != null ? request.thresholdPct() : DEFAULT_THRESHOLD;
        VarianceResult result = TargetCostVarianceEngine.calculate(
                new VarianceInput(target.getTargetAmount(), actual, threshold));

        TargetCostVariance entity = new TargetCostVariance();
        entity.setCustomerId(tenantAccessGuard.requireCustomerId());
        entity.setVarianceId(UUID.randomUUID().toString());
        entity.setTargetCostId(target.getTargetCostId());
        entity.setBomNodeId(target.getBomNodeId());
        entity.setProgramId(target.getProgramId());
        entity.setTargetAmount(target.getTargetAmount());
        entity.setActualAmount(actual);
        entity.setActualSource(source);
        entity.setActualSourceId(actualSourceId);
        entity.setAbsoluteVariance(result.absoluteVariance());
        entity.setPercentageVariance(result.percentageVariance());
        entity.setExceedsThreshold(result.exceedsThreshold());
        entity.setThresholdPct(threshold);
        entity.setCalculationBasis(result.calculationBasis());
        entity.setCalculatedAt(Instant.now());

        TargetCostVariance saved = varianceRepository.save(entity);
        auditService.recordCreate("TargetCostVariance", saved.getVarianceId(), toVarianceResponse(saved));
        return toVarianceResponse(saved);
    }

    @Transactional(readOnly = true)
    public TargetResponse getTarget(String targetCostId) {
        return toTargetResponse(getTargetOrThrow(targetCostId));
    }

    @Transactional(readOnly = true)
    public List<TargetResponse> listByBomNode(String bomNodeId) {
        BomNode node = bomNodeRepository.findByCustomerIdAndBomNodeId(tenantAccessGuard.requireCustomerId(), bomNodeId)
                .orElseThrow(() -> ResourceNotFoundException.of("BomNode", bomNodeId));
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, node.getProgramId());
        return targetRepository.findByCustomerIdAndBomNodeIdOrderByEffectiveFromDesc(tenantAccessGuard.requireCustomerId(), bomNodeId).stream()
                .map(this::toTargetResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TargetResponse> listByProgram(String programId) {
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, programId);
        return targetRepository.findByCustomerIdAndProgramIdOrderByEffectiveFromDesc(tenantAccessGuard.requireCustomerId(), programId).stream()
                .map(this::toTargetResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VarianceResponse> varianceHistory(String bomNodeId) {
        return varianceRepository.findByBomNodeIdOrderByCalculatedAtDesc(bomNodeId).stream()
                .map(this::toVarianceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExceedingVarianceSummary listExceedingThreshold() {
        List<VarianceResponse> all = varianceRepository.findByCustomerIdOrderByCalculatedAtDesc(tenantAccessGuard.requireCustomerId()).stream()
                .filter(TargetCostVariance::isExceedsThreshold)
                .sorted((a, b) -> b.getCalculatedAt().compareTo(a.getCalculatedAt()))
                .map(this::toVarianceResponse)
                .toList();
        return new ExceedingVarianceSummary(all, varianceRepository.countByExceedsThresholdTrue());
    }

    private TargetCostRecord getTargetOrThrow(String id) {
        return targetRepository.findByCustomerIdAndTargetCostId(tenantAccessGuard.requireCustomerId(), id)
                .orElseThrow(() -> ResourceNotFoundException.of("TargetCostRecord", id));
    }

    private TargetResponse toTargetResponse(TargetCostRecord r) {
        return new TargetResponse(
                r.getTargetCostId(), r.getBomNodeId(), r.getProgramId(),
                r.getTargetAmount(), r.getCurrencyCode(),
                r.getEffectiveFrom(), r.getEffectiveTo(),
                r.getStatus(), r.getApprovalStatus(), r.getNotes());
    }

    private VarianceResponse toVarianceResponse(TargetCostVariance v) {
        return new VarianceResponse(
                v.getVarianceId(), v.getTargetCostId(), v.getBomNodeId(), v.getProgramId(),
                v.getTargetAmount(), v.getActualAmount(), v.getActualSource(), v.getActualSourceId(),
                v.getAbsoluteVariance(), v.getPercentageVariance(), v.isExceedsThreshold(),
                v.getThresholdPct(), v.getCalculationBasis(), v.getCalculatedAt());
    }
}
