package com.automotive.costing.packaging.service;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.masterdata.entity.VehicleProgram;
import com.automotive.costing.masterdata.repository.VehicleProgramRepository;
import com.automotive.costing.packaging.dto.PackagingLogisticsDtos.*;
import com.automotive.costing.packaging.engine.PackagingLogisticsCostEngine;
import com.automotive.costing.packaging.engine.PackagingLogisticsCostEngine.PackagingLogisticsCostInput;
import com.automotive.costing.packaging.engine.PackagingLogisticsCostEngine.PackagingLogisticsCostResult;
import com.automotive.costing.packaging.entity.PackagingLogisticsCostCalculation;
import com.automotive.costing.packaging.entity.PackagingLogisticsProfile;
import com.automotive.costing.packaging.repository.PackagingLogisticsCostCalculationRepository;
import com.automotive.costing.packaging.repository.PackagingLogisticsProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * M07 Packaging & Logistics cost calculation.
 * Calculates, persists an append-only result, and exposes the latest total for
 * Should-Cost assembly (packaging_freight_cost element).
 */
@Service
@RequiredArgsConstructor
public class PackagingLogisticsCostService {

    private static final String ENTITY_TYPE = "PackagingLogisticsCostCalculation";
    private static final int MAX_COMPARE = 5;

    private final PackagingLogisticsProfileRepository profileRepository;
    private final PackagingLogisticsCostCalculationRepository calculationRepository;
    private final BomNodeRepository bomNodeRepository;
    private final VehicleProgramRepository vehicleProgramRepository;
    private final AuditService auditService;

    @Transactional
    public CalculationResponse calculate(CalculateRequest request) {
        PackagingLogisticsProfile profile = profileRepository.findByProfileId(request.profileId())
                .orElseThrow(() -> ResourceNotFoundException.of("PackagingLogisticsProfile", request.profileId()));
        if (!profile.isActive()) {
            throw new BusinessRuleViolationException(
                    "Cannot calculate cost for an inactive packaging profile", "PKG-05");
        }

        BomNode bomNode = bomNodeRepository.findByBomNodeId(profile.getBomNodeId())
                .orElseThrow(() -> ResourceNotFoundException.of("BomNode", profile.getBomNodeId()));
        VehicleProgram program = vehicleProgramRepository.findById(bomNode.getProgramId())
                .orElseThrow(() -> ResourceNotFoundException.of("VehicleProgram", bomNode.getProgramId()));

        PackagingLogisticsCostResult result = PackagingLogisticsCostEngine.calculate(
                new PackagingLogisticsCostInput(
                        profile.getPrimaryPackCostPerPiece(),
                        profile.getSecondaryPackCostPerPiece(),
                        profile.getReturnableContainerCost(),
                        profile.getContainerLifeCycles(),
                        profile.getNetWeightKg(),
                        profile.getFreightMode(),
                        profile.getFreightRatePerKg(),
                        profile.getFixedFreightPerUnit(),
                        profile.getDutyPct()));

        PackagingLogisticsCostCalculation entity = new PackagingLogisticsCostCalculation();
        entity.setCalculationId(UUID.randomUUID().toString());
        entity.setProfileId(profile.getProfileId());
        entity.setBomNodeId(profile.getBomNodeId());
        entity.setProgramId(program.getProgramId());
        entity.setPackagingCostPerPiece(result.packagingCostPerPiece());
        entity.setFreightCostPerPiece(result.freightCostPerPiece());
        entity.setDutyCostPerPiece(result.dutyCostPerPiece());
        entity.setTotalPackagingFreightCost(result.totalPackagingFreightCost());
        entity.setCalculationBasis(result.calculationBasis());
        entity.setCalculatedAt(Instant.now());

        PackagingLogisticsCostCalculation saved = calculationRepository.save(entity);
        auditService.recordCreate(ENTITY_TYPE, saved.getCalculationId(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CalculationResponse latestForBomNode(String bomNodeId) {
        return calculationRepository.findFirstByBomNodeIdOrderByCalculatedAtDesc(bomNodeId)
                .map(this::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of(
                        "PackagingLogisticsCostCalculation for BomNode", bomNodeId));
    }

    @Transactional(readOnly = true)
    public List<CalculationResponse> historyForBomNode(String bomNodeId) {
        bomNodeRepository.findByBomNodeId(bomNodeId)
                .orElseThrow(() -> ResourceNotFoundException.of("BomNode", bomNodeId));
        return calculationRepository.findByBomNodeIdOrderByCalculatedAtDesc(bomNodeId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Compare total packaging+freight cost across up to five profiles (alternate packaging strategies). */
    @Transactional(readOnly = true)
    public CompareResponse compare(CompareRequest request) {
        if (request.profileIds() == null || request.profileIds().size() < 2) {
            throw new BusinessRuleViolationException(
                    "At least two packaging profiles are required for comparison", "PKG-06");
        }
        if (request.profileIds().size() > MAX_COMPARE) {
            throw new BusinessRuleViolationException(
                    "Compare supports up to " + MAX_COMPARE + " profiles at a time", "PKG-06");
        }

        List<CompareLine> lines = request.profileIds().stream()
                .map(this::compareOne)
                .sorted(Comparator.comparing(CompareLine::totalPackagingFreightCost))
                .toList();
        String lowest = lines.isEmpty() ? null : lines.get(0).profileId();
        return new CompareResponse(lines, lowest);
    }

    private CompareLine compareOne(String profileId) {
        PackagingLogisticsProfile profile = profileRepository.findByProfileId(profileId)
                .orElseThrow(() -> ResourceNotFoundException.of("PackagingLogisticsProfile", profileId));
        PackagingLogisticsCostResult result = PackagingLogisticsCostEngine.calculate(
                new PackagingLogisticsCostInput(
                        profile.getPrimaryPackCostPerPiece(),
                        profile.getSecondaryPackCostPerPiece(),
                        profile.getReturnableContainerCost(),
                        profile.getContainerLifeCycles(),
                        profile.getNetWeightKg(),
                        profile.getFreightMode(),
                        profile.getFreightRatePerKg(),
                        profile.getFixedFreightPerUnit(),
                        profile.getDutyPct()));
        return new CompareLine(
                profile.getProfileId(),
                profile.getProfileLabel(),
                profile.getPackagingType(),
                result.totalPackagingFreightCost());
    }

    private CalculationResponse toResponse(PackagingLogisticsCostCalculation c) {
        return new CalculationResponse(
                c.getCalculationId(),
                c.getProfileId(),
                c.getBomNodeId(),
                c.getProgramId(),
                c.getPackagingCostPerPiece(),
                c.getFreightCostPerPiece(),
                c.getDutyCostPerPiece(),
                c.getTotalPackagingFreightCost(),
                c.getCalculationBasis(),
                c.getCalculatedAt());
    }
}
