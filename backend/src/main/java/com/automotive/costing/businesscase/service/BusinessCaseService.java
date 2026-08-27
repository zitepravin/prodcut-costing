package com.automotive.costing.businesscase.service;

import com.automotive.costing.businesscase.dto.BusinessCaseDtos.CreateScenarioRequest;
import com.automotive.costing.businesscase.dto.BusinessCaseDtos.ScenarioResponse;
import com.automotive.costing.businesscase.entity.BusinessCaseScenario;
import com.automotive.costing.businesscase.entity.BusinessCaseStatus;
import com.automotive.costing.businesscase.repository.BusinessCaseScenarioRepository;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.masterdata.repository.VehicleProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Program-level contribution margin + simple NPV/payback.
 * Complements should-cost; does not post to ERP GL or replace Material Ledger.
 */
@Service
@RequiredArgsConstructor
public class BusinessCaseService {

    private static final int SCALE = 6;
    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);

    private final BusinessCaseScenarioRepository repository;
    private final VehicleProgramRepository vehicleProgramRepository;

    @Transactional(readOnly = true)
    public List<ScenarioResponse> listByProgram(String programId) {
        return repository.findByProgramIdOrderByScenarioCodeAsc(programId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScenarioResponse get(String scenarioId) {
        return toResponse(require(scenarioId));
    }

    @Transactional
    public ScenarioResponse create(CreateScenarioRequest req) {
        if (!vehicleProgramRepository.existsById(req.programId().trim())) {
            throw ResourceNotFoundException.of("VehicleProgram", req.programId());
        }
        repository.findByProgramIdAndScenarioCode(req.programId().trim(), req.scenarioCode().trim())
                .ifPresent(s -> {
                    throw new BusinessRuleViolationException(
                            "Scenario code already exists for program", "BC_SCENARIO_EXISTS");
                });

        BusinessCaseScenario s = new BusinessCaseScenario();
        s.setScenarioId(UUID.randomUUID().toString());
        s.setProgramId(req.programId().trim());
        s.setScenarioCode(req.scenarioCode().trim());
        s.setScenarioName(req.scenarioName().trim());
        s.setStatus(BusinessCaseStatus.DRAFT);
        s.setCurrencyCode(req.currencyCode() == null || req.currencyCode().isBlank()
                ? "USD" : req.currencyCode().trim().toUpperCase());
        s.setUnitSellingPrice(scale(req.unitSellingPrice()));
        s.setUnitShouldCost(scale(req.unitShouldCost()));
        s.setUnitVariableCost(scale(req.unitVariableCost() != null ? req.unitVariableCost() : req.unitShouldCost()));
        s.setAnnualVolume(scale(req.annualVolume()));
        s.setHorizonYears(req.horizonYears() != null && req.horizonYears() > 0 ? req.horizonYears() : 5);
        s.setDiscountRatePct(scale(req.discountRatePct() != null ? req.discountRatePct() : new BigDecimal("0.10")));
        s.setInitialInvestment(scale(req.initialInvestment() != null ? req.initialInvestment() : BigDecimal.ZERO));
        s.setFixedAnnualCost(scale(req.fixedAnnualCost() != null ? req.fixedAnnualCost() : BigDecimal.ZERO));
        return toResponse(repository.save(s));
    }

    @Transactional
    public ScenarioResponse calculate(String scenarioId) {
        BusinessCaseScenario s = require(scenarioId);
        if (s.getAnnualVolume().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException("Annual volume cannot be negative", "BC_VOLUME_NEGATIVE");
        }
        if (s.getHorizonYears() < 1 || s.getHorizonYears() > 50) {
            throw new BusinessRuleViolationException("Horizon years must be 1–50", "BC_HORIZON_RANGE");
        }

        // Unit contribution = price − variable cost (should-cost used as default variable)
        BigDecimal cmUnit = s.getUnitSellingPrice().subtract(s.getUnitVariableCost()).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal annualCm = cmUnit.multiply(s.getAnnualVolume()).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal annualNet = annualCm.subtract(s.getFixedAnnualCost()).setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal rate = s.getDiscountRatePct();
        BigDecimal npv = s.getInitialInvestment().negate();
        for (int y = 1; y <= s.getHorizonYears(); y++) {
            BigDecimal discount = BigDecimal.ONE.add(rate).pow(y, MC);
            BigDecimal pv = annualNet.divide(discount, MC).setScale(SCALE, RoundingMode.HALF_UP);
            npv = npv.add(pv);
        }

        BigDecimal payback = null;
        if (annualNet.compareTo(BigDecimal.ZERO) > 0 && s.getInitialInvestment().compareTo(BigDecimal.ZERO) > 0) {
            payback = s.getInitialInvestment().divide(annualNet, MC).setScale(SCALE, RoundingMode.HALF_UP);
        }

        String basis = "CM_unit=price-variable; annual_CM=CM_unit*volume; annual_net=annual_CM-fixed; "
                + "NPV=sum(annual_net/(1+r)^y)-investment; payback=investment/annual_net when annual_net>0. "
                + "Not ERP P&L; not Material Ledger.";

        s.setContributionMarginUnit(cmUnit);
        s.setAnnualContribution(annualCm);
        s.setNpvAmount(npv.setScale(SCALE, RoundingMode.HALF_UP));
        s.setPaybackYears(payback);
        s.setCalculationBasis(basis);
        s.setCalculatedAt(Instant.now());
        s.setStatus(BusinessCaseStatus.CALCULATED);
        return toResponse(repository.save(s));
    }

    private BusinessCaseScenario require(String id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("BusinessCaseScenario", id));
    }

    private ScenarioResponse toResponse(BusinessCaseScenario s) {
        return new ScenarioResponse(
                s.getScenarioId(), s.getProgramId(), s.getScenarioCode(), s.getScenarioName(),
                s.getStatus(), s.getCurrencyCode(),
                s.getUnitSellingPrice(), s.getUnitShouldCost(), s.getUnitVariableCost(),
                s.getAnnualVolume(), s.getHorizonYears(), s.getDiscountRatePct(),
                s.getInitialInvestment(), s.getFixedAnnualCost(),
                s.getContributionMarginUnit(), s.getAnnualContribution(),
                s.getNpvAmount(), s.getPaybackYears(),
                s.getCalculationBasis(), s.getCalculatedAt()
        );
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
