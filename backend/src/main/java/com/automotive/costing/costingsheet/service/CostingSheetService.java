package com.automotive.costing.costingsheet.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.costingsheet.dto.CostingSheetDtos.*;
import com.automotive.costing.costingsheet.engine.CostingSheetEngine;
import com.automotive.costing.costingsheet.engine.CostingSheetEngine.CostBase;
import com.automotive.costing.costingsheet.engine.CostingSheetEngine.SheetResult;
import com.automotive.costing.costingsheet.entity.*;
import com.automotive.costing.costingsheet.repository.CostingSheetRepository;
import com.automotive.costing.costingsheet.repository.CostingSheetRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CostingSheetService {

    private final CostingSheetRepository sheetRepository;
    private final CostingSheetRuleRepository ruleRepository;

    @Transactional(readOnly = true)
    public List<SheetResponse> list() {
        return sheetRepository.findAllByOrderBySheetCodeAsc().stream().map(this::toSheet).toList();
    }

    @Transactional(readOnly = true)
    public SheetDetailResponse get(String sheetId) {
        CostingSheet sheet = require(sheetId);
        List<RuleResponse> rules = ruleRepository.findBySheetIdOrderBySequenceNoAsc(sheetId).stream()
                .map(this::toRule)
                .toList();
        return new SheetDetailResponse(toSheet(sheet), rules);
    }

    @Transactional
    public SheetResponse createSheet(CreateSheetRequest req) {
        sheetRepository.findBySheetCode(req.sheetCode().trim()).ifPresent(s -> {
            throw new BusinessRuleViolationException("Sheet code exists: " + req.sheetCode(), "CS_CODE_EXISTS");
        });
        CostingSheet s = new CostingSheet();
        s.setSheetId(UUID.randomUUID().toString());
        s.setSheetCode(req.sheetCode().trim());
        s.setSheetName(req.sheetName().trim());
        s.setStatus(CostingSheetStatus.DRAFT);
        s.setPlantCode(trimToNull(req.plantCode()));
        s.setCurrencyCode(req.currencyCode() == null || req.currencyCode().isBlank()
                ? "USD" : req.currencyCode().trim().toUpperCase());
        s.setValidFrom(req.validFrom());
        s.setValidTo(req.validTo());
        s.setDescription(trimToNull(req.description()));
        return toSheet(sheetRepository.save(s));
    }

    @Transactional
    public RuleResponse addRule(String sheetId, CreateRuleRequest req) {
        CostingSheet sheet = require(sheetId);
        if (sheet.getStatus() == CostingSheetStatus.RETIRED) {
            throw new BusinessRuleViolationException("Cannot add rules to RETIRED sheet", "CS_RETIRED");
        }
        if (req.ruleType() == CostingSheetRuleType.PERCENT_OF_BASE) {
            if (req.baseCode() == null) {
                throw new BusinessRuleViolationException("base_code required for PERCENT_OF_BASE", "CS_BASE_REQUIRED");
            }
            if (req.percentRate() == null) {
                throw new BusinessRuleViolationException("percent_rate required", "CS_PCT_REQUIRED");
            }
        }
        if (req.ruleType() == CostingSheetRuleType.FIXED_AMOUNT && req.fixedAmount() == null) {
            throw new BusinessRuleViolationException("fixed_amount required", "CS_FIXED_REQUIRED");
        }
        CostingSheetRule r = new CostingSheetRule();
        r.setRuleId(UUID.randomUUID().toString());
        r.setSheetId(sheetId);
        r.setSequenceNo(req.sequenceNo());
        r.setRuleCode(req.ruleCode().trim());
        r.setRuleName(req.ruleName().trim());
        r.setRuleType(req.ruleType());
        r.setBaseCode(req.baseCode());
        r.setPercentRate(req.percentRate());
        r.setFixedAmount(req.fixedAmount());
        r.setComponentCode(req.componentCode().trim());
        r.setCostBehavior(req.costBehavior() == null || req.costBehavior().isBlank()
                ? "FIXED" : req.costBehavior().trim());
        r.setActive(true);
        return toRule(ruleRepository.save(r));
    }

    @Transactional
    public SheetResponse activate(String sheetId) {
        CostingSheet s = require(sheetId);
        if (s.getStatus() == CostingSheetStatus.RETIRED) {
            throw new BusinessRuleViolationException("Cannot activate RETIRED sheet", "CS_RETIRED");
        }
        if (ruleRepository.countBySheetId(sheetId) == 0) {
            throw new BusinessRuleViolationException("Activate requires at least one rule", "CS_EMPTY");
        }
        s.setStatus(CostingSheetStatus.ACTIVE);
        return toSheet(sheetRepository.save(s));
    }

    @Transactional
    public SheetResponse retire(String sheetId) {
        CostingSheet s = require(sheetId);
        s.setStatus(CostingSheetStatus.RETIRED);
        return toSheet(sheetRepository.save(s));
    }

    /**
     * Apply ACTIVE sheet rules to a cost base — returns audited line results.
     * Pure calculation via CostingSheetEngine.
     */
    @Transactional(readOnly = true)
    public ApplyResponse apply(ApplyRequest req) {
        CostingSheet sheet = require(req.sheetId());
        if (sheet.getStatus() != CostingSheetStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                    "Only ACTIVE sheets can be applied: " + sheet.getSheetCode(), "CS_NOT_ACTIVE");
        }
        LocalDate today = LocalDate.now();
        if (sheet.getValidFrom() != null && today.isBefore(sheet.getValidFrom())) {
            throw new BusinessRuleViolationException("Sheet not yet valid", "CS_NOT_YET_VALID");
        }
        if (sheet.getValidTo() != null && today.isAfter(sheet.getValidTo())) {
            throw new BusinessRuleViolationException("Sheet expired", "CS_EXPIRED");
        }
        List<CostingSheetRule> rules = ruleRepository.findBySheetIdAndActiveTrueOrderBySequenceNoAsc(sheet.getSheetId());
        CostBase base = new CostBase(
                req.material(),
                req.process(),
                req.tooling() != null ? req.tooling() : BigDecimal.ZERO,
                req.packaging() != null ? req.packaging() : BigDecimal.ZERO
        );
        SheetResult result = CostingSheetEngine.apply(base, rules);
        List<AppliedLineResponse> lines = result.lines().stream()
                .map(l -> new AppliedLineResponse(
                        l.sequenceNo(), l.ruleCode(), l.ruleName(), l.ruleType(), l.baseCode(),
                        l.baseAmount(), l.rateOrFixed(), l.amount(), l.componentCode(),
                        l.costBehavior(), l.calculationRule()))
                .toList();
        return new ApplyResponse(
                sheet.getSheetId(),
                sheet.getSheetCode(),
                lines,
                base.subtotal(),
                result.totalOverhead(),
                result.grandTotal(),
                result.calculationBasis()
        );
    }

    private CostingSheet require(String id) {
        return sheetRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("CostingSheet", id));
    }

    private SheetResponse toSheet(CostingSheet s) {
        return new SheetResponse(
                s.getSheetId(), s.getSheetCode(), s.getSheetName(), s.getStatus(),
                s.getPlantCode(), s.getCurrencyCode(), s.getValidFrom(), s.getValidTo(),
                s.getDescription(), ruleRepository.countBySheetId(s.getSheetId())
        );
    }

    private RuleResponse toRule(CostingSheetRule r) {
        return new RuleResponse(
                r.getRuleId(), r.getSequenceNo(), r.getRuleCode(), r.getRuleName(),
                r.getRuleType(), r.getBaseCode(), r.getPercentRate(), r.getFixedAmount(),
                r.getComponentCode(), r.getCostBehavior(), r.isActive()
        );
    }

    private static String trimToNull(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }
}
