package com.automotive.costing.standardcost.service;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import com.automotive.costing.standardcost.dto.StandardCostDtos.*;
import com.automotive.costing.standardcost.entity.StandardCost;
import com.automotive.costing.standardcost.entity.StandardCostStatus;
import com.automotive.costing.standardcost.repository.StandardCostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/**
 * C1 Standard Cost Lifecycle.
 * DRAFT → MARKED_FOR_RELEASE → RELEASED → FROZEN → SUPERSEDED
 * Separate object from should-cost draft; never mutates locked should-cost amounts in place.
 */
@Service
@RequiredArgsConstructor
public class StandardCostService {

    private static final int SCALE = 6;

    private final StandardCostRepository repository;
    private final ShouldCostBuildUpRepository shouldCostBuildUpRepository;
    private final BomNodeRepository bomNodeRepository;

    @Transactional(readOnly = true)
    public List<StandardCostResponse> listByBomNode(String bomNodeId) {
        return repository.findByBomNodeIdOrderByVersionNoDesc(bomNodeId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StandardCostResponse get(String id) {
        return toResponse(require(id));
    }

    @Transactional(readOnly = true)
    public LifecycleHistoryResponse history(String bomNodeId) {
        List<StandardCostResponse> versions = listByBomNode(bomNodeId);
        StandardCostResponse current = repository
                .findFirstByBomNodeIdAndStatusOrderByVersionNoDesc(bomNodeId, StandardCostStatus.RELEASED)
                .or(() -> repository.findFirstByBomNodeIdAndStatusOrderByVersionNoDesc(bomNodeId, StandardCostStatus.FROZEN))
                .map(this::toResponse)
                .orElse(null);
        return new LifecycleHistoryResponse(bomNodeId, versions, current);
    }

    /** Snapshot should-cost into a new DRAFT standard-cost version. */
    @Transactional
    public StandardCostResponse createFromShouldCost(CreateFromShouldCostRequest req) {
        ShouldCostBuildUp sc = shouldCostBuildUpRepository.findById(req.shouldCostBuildUpId().trim())
                .orElseThrow(() -> ResourceNotFoundException.of("ShouldCostBuildUp", req.shouldCostBuildUpId()));
        BomNode node = bomNodeRepository.findByBomNodeId(sc.getBomNodeId())
                .orElseThrow(() -> ResourceNotFoundException.of("BomNode", sc.getBomNodeId()));

        if (sc.getTotalCost() == null) {
            throw new BusinessRuleViolationException(
                    "Should-cost has no total cost — cannot create standard cost", "STD_NO_TOTAL");
        }

        StandardCost s = new StandardCost();
        s.setStandardCostId(java.util.UUID.randomUUID().toString());
        s.setBomNodeId(node.getBomNodeId());
        s.setProgramId(node.getProgramId());
        s.setVersionNo(repository.maxVersionNo(node.getBomNodeId()) + 1);
        s.setStatus(StandardCostStatus.DRAFT);
        s.setSourceType("SHOULD_COST");
        s.setSourceRefId(sc.getBuildUpId());
        s.setUnitCost(sc.getTotalCost().setScale(SCALE, RoundingMode.HALF_UP));
        s.setCurrencyCode("USD");
        s.setMaterialCost(sc.getMaterialCost());
        s.setProcessCost(sc.getProcessCost());
        s.setToolingCost(sc.getToolingCost());
        s.setPackagingCost(sc.getPackagingFreightCost());
        s.setOverheadCost(sc.getOverheadCost());
        s.setSgaCost(sc.getSgaCost());
        s.setProfitMargin(sc.getProfitMargin());
        s.setPlantCode(trimToNull(req.plantCode()));
        s.setValidFrom(req.validFrom());
        s.setValidTo(req.validTo());
        s.setCalculationBasis("C1 snapshot from should-cost " + sc.getBuildUpId()
                + (sc.getCalculationBasis() != null ? " | " + sc.getCalculationBasis() : ""));
        if (s.getCalculationBasis().length() > 2000) {
            s.setCalculationBasis(s.getCalculationBasis().substring(0, 2000));
        }
        s.setNotes(trimToNull(req.notes()));
        return toResponse(repository.save(s));
    }

    @Transactional
    public StandardCostResponse markForRelease(String id) {
        StandardCost s = require(id);
        assertStatus(s, StandardCostStatus.DRAFT, "Only DRAFT can be marked for release");
        s.setStatus(StandardCostStatus.MARKED_FOR_RELEASE);
        return toResponse(repository.save(s));
    }

    /**
     * Release becomes the current operational standard cost for the BOM node.
     * Previous RELEASED versions are SUPERSEDED (not deleted).
     */
    @Transactional
    public StandardCostResponse release(String id, String releasedBy) {
        StandardCost s = require(id);
        if (s.getStatus() != StandardCostStatus.DRAFT && s.getStatus() != StandardCostStatus.MARKED_FOR_RELEASE) {
            throw new BusinessRuleViolationException(
                    "Only DRAFT or MARKED_FOR_RELEASE can be released (current=" + s.getStatus() + ")",
                    "STD_RELEASE_STATE");
        }

        // Supersede any currently RELEASED versions for same BOM node
        List<StandardCost> currentReleased = repository.findByBomNodeIdAndStatusIn(
                s.getBomNodeId(), List.of(StandardCostStatus.RELEASED));
        for (StandardCost prev : currentReleased) {
            prev.setStatus(StandardCostStatus.SUPERSEDED);
            prev.setSupersededById(s.getStandardCostId());
            repository.save(prev);
        }

        s.setStatus(StandardCostStatus.RELEASED);
        s.setReleasedAt(Instant.now());
        s.setReleasedBy(releasedBy);
        return toResponse(repository.save(s));
    }

    /** Freeze = permanent audit baseline; cannot release another over it without new version. */
    @Transactional
    public StandardCostResponse freeze(String id, String frozenBy) {
        StandardCost s = require(id);
        assertStatus(s, StandardCostStatus.RELEASED, "Only RELEASED can be frozen");
        s.setStatus(StandardCostStatus.FROZEN);
        s.setFrozenAt(Instant.now());
        s.setFrozenBy(frozenBy);
        return toResponse(repository.save(s));
    }

    /** Explicit supersede of a RELEASED/FROZEN record by pointing to a newer RELEASED id. */
    @Transactional
    public StandardCostResponse supersede(String id, String successorId) {
        StandardCost s = require(id);
        StandardCost successor = require(successorId);
        if (!s.getBomNodeId().equals(successor.getBomNodeId())) {
            throw new BusinessRuleViolationException("Successor must be same BOM node", "STD_BOM_MISMATCH");
        }
        if (s.getStatus() != StandardCostStatus.RELEASED && s.getStatus() != StandardCostStatus.FROZEN) {
            throw new BusinessRuleViolationException(
                    "Only RELEASED or FROZEN can be superseded", "STD_SUPERSEDE_STATE");
        }
        s.setStatus(StandardCostStatus.SUPERSEDED);
        s.setSupersededById(successor.getStandardCostId());
        return toResponse(repository.save(s));
    }

    private void assertStatus(StandardCost s, StandardCostStatus expected, String message) {
        if (s.getStatus() != expected) {
            throw new BusinessRuleViolationException(message + " (current=" + s.getStatus() + ")", "STD_STATE");
        }
    }

    private StandardCost require(String id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("StandardCost", id));
    }

    private StandardCostResponse toResponse(StandardCost s) {
        return new StandardCostResponse(
                s.getStandardCostId(), s.getBomNodeId(), s.getProgramId(), s.getVersionNo(),
                s.getStatus(), s.getSourceType(), s.getSourceRefId(),
                s.getUnitCost(), s.getCurrencyCode(),
                s.getMaterialCost(), s.getProcessCost(), s.getToolingCost(), s.getPackagingCost(),
                s.getOverheadCost(), s.getSgaCost(), s.getProfitMargin(),
                s.getCostingVariantId(), s.getPlantCode(), s.getValidFrom(), s.getValidTo(),
                s.getReleasedAt(), s.getReleasedBy(), s.getFrozenAt(), s.getFrozenBy(),
                s.getSupersededById(), s.getCalculationBasis(), s.getNotes()
        );
    }

    private static String trimToNull(String v) {
        if (v == null || v.isBlank()) return null;
        return v.trim();
    }
}
