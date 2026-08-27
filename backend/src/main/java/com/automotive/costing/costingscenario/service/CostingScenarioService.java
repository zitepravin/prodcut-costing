package com.automotive.costing.costingscenario.service;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.costingscenario.dto.CostingScenarioDtos.*;
import com.automotive.costing.costingscenario.entity.*;
import com.automotive.costing.costingscenario.repository.AdditiveCostLineRepository;
import com.automotive.costing.costingscenario.repository.CostingScenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * B4 Scenario costing — typed scenarios that never overwrite APPROVED/locked estimates.
 * B5 Additive costs — separately identifiable one-time / special cost lines.
 */
@Service
@RequiredArgsConstructor
public class CostingScenarioService {

    private static final int SCALE = 6;

    private final CostingScenarioRepository scenarioRepository;
    private final AdditiveCostLineRepository additiveRepository;
    private final BomNodeRepository bomNodeRepository;

    @Transactional(readOnly = true)
    public List<ScenarioResponse> listByBomNode(String bomNodeId) {
        return scenarioRepository.findByBomNodeIdOrderByScenarioCodeAsc(bomNodeId).stream()
                .map(this::toScenario)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScenarioDetailResponse get(String scenarioId) {
        CostingScenario s = require(scenarioId);
        List<AdditiveLineResponse> lines = additiveRepository.findByScenarioIdOrderByLineCodeAsc(scenarioId)
                .stream().map(this::toLine).toList();
        return new ScenarioDetailResponse(toScenario(s), lines);
    }

    @Transactional
    public ScenarioResponse create(CreateScenarioRequest req) {
        BomNode node = bomNodeRepository.findByBomNodeId(req.bomNodeId().trim())
                .orElseThrow(() -> ResourceNotFoundException.of("BomNode", req.bomNodeId()));

        scenarioRepository.findByBomNodeIdAndScenarioCode(node.getBomNodeId(), req.scenarioCode().trim())
                .ifPresent(x -> {
                    throw new BusinessRuleViolationException(
                            "Scenario code already exists on this BOM node", "SCN_CODE_EXISTS");
                });

        if (req.parentScenarioId() != null && !req.parentScenarioId().isBlank()) {
            require(req.parentScenarioId().trim());
        }

        CostingScenario s = new CostingScenario();
        s.setScenarioId(UUID.randomUUID().toString());
        s.setBomNodeId(node.getBomNodeId());
        s.setProgramId(req.programId() != null && !req.programId().isBlank()
                ? req.programId().trim() : node.getProgramId());
        s.setScenarioCode(req.scenarioCode().trim());
        s.setScenarioName(req.scenarioName().trim());
        s.setScenarioType(req.scenarioType());
        s.setStatus(ScenarioStatus.DRAFT);
        s.setParentScenarioId(trimToNull(req.parentScenarioId()));
        s.setShouldCostBuildUpId(trimToNull(req.shouldCostBuildUpId()));
        s.setCostingVariantId(trimToNull(req.costingVariantId()));
        s.setNotes(trimToNull(req.notes()));
        s.setLocked(false);
        return toScenario(scenarioRepository.save(s));
    }

    /**
     * Branch from an existing scenario into a new DRAFT — never mutates APPROVED source.
     */
    @Transactional
    public ScenarioResponse branch(BranchScenarioRequest req) {
        CostingScenario source = require(req.sourceScenarioId());
        scenarioRepository.findByBomNodeIdAndScenarioCode(source.getBomNodeId(), req.newScenarioCode().trim())
                .ifPresent(x -> {
                    throw new BusinessRuleViolationException(
                            "New scenario code already exists", "SCN_CODE_EXISTS");
                });

        CostingScenario s = new CostingScenario();
        s.setScenarioId(UUID.randomUUID().toString());
        s.setBomNodeId(source.getBomNodeId());
        s.setProgramId(source.getProgramId());
        s.setScenarioCode(req.newScenarioCode().trim());
        s.setScenarioName(req.newScenarioName().trim());
        s.setScenarioType(req.scenarioType());
        s.setStatus(ScenarioStatus.DRAFT);
        s.setParentScenarioId(source.getScenarioId());
        s.setShouldCostBuildUpId(source.getShouldCostBuildUpId());
        s.setCostingVariantId(source.getCostingVariantId());
        s.setNotes(trimToNull(req.notes()));
        s.setLocked(false);
        CostingScenario saved = scenarioRepository.save(s);

        // Copy additive lines into the branch (editable on DRAFT)
        for (AdditiveCostLine src : additiveRepository.findByScenarioIdOrderByLineCodeAsc(source.getScenarioId())) {
            AdditiveCostLine copy = new AdditiveCostLine();
            copy.setLineId(UUID.randomUUID().toString());
            copy.setScenarioId(saved.getScenarioId());
            copy.setLineCode(src.getLineCode());
            copy.setLineName(src.getLineName());
            copy.setAdditiveType(src.getAdditiveType());
            copy.setAmount(src.getAmount());
            copy.setCurrencyCode(src.getCurrencyCode());
            copy.setAmortized(src.isAmortized());
            copy.setAmortizationVolume(src.getAmortizationVolume());
            copy.setAmountPerUnit(src.getAmountPerUnit());
            copy.setNotes(src.getNotes());
            copy.setCreatedAt(Instant.now());
            additiveRepository.save(copy);
        }
        return toScenario(saved);
    }

    @Transactional
    public ScenarioResponse activate(String scenarioId) {
        CostingScenario s = requireEditable(scenarioId);
        s.setStatus(ScenarioStatus.ACTIVE);
        return toScenario(scenarioRepository.save(s));
    }

    /**
     * Approve locks the scenario — no further additive/link edits.
     * Does not modify any other APPROVED scenario (historical integrity).
     */
    @Transactional
    public ScenarioResponse approve(String scenarioId) {
        CostingScenario s = require(scenarioId);
        if (s.getStatus() == ScenarioStatus.SUPERSEDED) {
            throw new BusinessRuleViolationException("Cannot approve SUPERSEDED scenario", "SCN_SUPERSEDED");
        }
        s.setStatus(ScenarioStatus.APPROVED);
        s.setLocked(true);
        return toScenario(scenarioRepository.save(s));
    }

    @Transactional
    public ScenarioResponse linkShouldCost(String scenarioId, String buildUpId) {
        CostingScenario s = requireEditable(scenarioId);
        s.setShouldCostBuildUpId(buildUpId.trim());
        return toScenario(scenarioRepository.save(s));
    }

    @Transactional
    public AdditiveLineResponse addAdditiveLine(String scenarioId, AddAdditiveLineRequest req) {
        requireEditable(scenarioId);
        if (req.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException("Additive amount cannot be negative", "ADD_NEG");
        }

        AdditiveCostLine line = new AdditiveCostLine();
        line.setLineId(UUID.randomUUID().toString());
        line.setScenarioId(scenarioId);
        line.setLineCode(req.lineCode().trim());
        line.setLineName(req.lineName().trim());
        line.setAdditiveType(req.additiveType());
        line.setAmount(req.amount().setScale(SCALE, RoundingMode.HALF_UP));
        line.setCurrencyCode(req.currencyCode() == null || req.currencyCode().isBlank()
                ? "USD" : req.currencyCode().trim().toUpperCase());
        boolean amort = Boolean.TRUE.equals(req.amortized());
        line.setAmortized(amort);
        if (amort) {
            if (req.amortizationVolume() == null || req.amortizationVolume().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleViolationException(
                        "Amortized additive requires amortization_volume > 0", "ADD_AMORT_VOL");
            }
            line.setAmortizationVolume(req.amortizationVolume().setScale(SCALE, RoundingMode.HALF_UP));
            line.setAmountPerUnit(line.getAmount()
                    .divide(line.getAmortizationVolume(), SCALE, RoundingMode.HALF_UP));
        } else {
            line.setAmortizationVolume(null);
            line.setAmountPerUnit(null);
        }
        line.setNotes(trimToNull(req.notes()));
        line.setCreatedAt(Instant.now());
        return toLine(additiveRepository.save(line));
    }

    @Transactional(readOnly = true)
    public ScenarioCompareResponse compare(String bomNodeId) {
        List<ScenarioResponse> list = listByBomNode(bomNodeId);
        return new ScenarioCompareResponse(
                bomNodeId,
                list,
                "Scenarios are independent records; APPROVED scenarios are never overwritten — branch to change."
        );
    }

    private CostingScenario requireEditable(String scenarioId) {
        CostingScenario s = require(scenarioId);
        if (s.isLocked() || s.getStatus() == ScenarioStatus.APPROVED) {
            throw new BusinessRuleViolationException(
                    "APPROVED/locked scenario cannot be modified — branch a new scenario instead",
                    "SCN_LOCKED");
        }
        if (s.getStatus() == ScenarioStatus.SUPERSEDED) {
            throw new BusinessRuleViolationException("SUPERSEDED scenario is read-only", "SCN_SUPERSEDED");
        }
        return s;
    }

    private CostingScenario require(String id) {
        return scenarioRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("CostingScenario", id));
    }

    private ScenarioResponse toScenario(CostingScenario s) {
        List<AdditiveCostLine> lines = additiveRepository.findByScenarioIdOrderByLineCodeAsc(s.getScenarioId());
        BigDecimal total = lines.stream().map(AdditiveCostLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal perUnit = lines.stream()
                .map(l -> l.getAmountPerUnit() != null ? l.getAmountPerUnit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(SCALE, RoundingMode.HALF_UP);
        return new ScenarioResponse(
                s.getScenarioId(), s.getBomNodeId(), s.getProgramId(),
                s.getScenarioCode(), s.getScenarioName(), s.getScenarioType(), s.getStatus(),
                s.getParentScenarioId(), s.getShouldCostBuildUpId(), s.getCostingVariantId(),
                s.getNotes(), s.isLocked(), total, perUnit, lines.size()
        );
    }

    private AdditiveLineResponse toLine(AdditiveCostLine l) {
        return new AdditiveLineResponse(
                l.getLineId(), l.getLineCode(), l.getLineName(), l.getAdditiveType(),
                l.getAmount(), l.getCurrencyCode(), l.isAmortized(),
                l.getAmortizationVolume(), l.getAmountPerUnit(), l.getNotes()
        );
    }

    private static String trimToNull(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }
}
