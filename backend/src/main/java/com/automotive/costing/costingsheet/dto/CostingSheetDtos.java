package com.automotive.costing.costingsheet.dto;

import com.automotive.costing.costingsheet.entity.CostingSheetBaseCode;
import com.automotive.costing.costingsheet.entity.CostingSheetRuleType;
import com.automotive.costing.costingsheet.entity.CostingSheetStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class CostingSheetDtos {
    private CostingSheetDtos() {}

    public record SheetResponse(
            String sheetId,
            String sheetCode,
            String sheetName,
            CostingSheetStatus status,
            String plantCode,
            String currencyCode,
            LocalDate validFrom,
            LocalDate validTo,
            String description,
            long ruleCount
    ) {}

    public record RuleResponse(
            String ruleId,
            int sequenceNo,
            String ruleCode,
            String ruleName,
            CostingSheetRuleType ruleType,
            CostingSheetBaseCode baseCode,
            BigDecimal percentRate,
            BigDecimal fixedAmount,
            String componentCode,
            String costBehavior,
            boolean active
    ) {}

    public record SheetDetailResponse(SheetResponse sheet, List<RuleResponse> rules) {}

    public record CreateSheetRequest(
            @NotBlank String sheetCode,
            @NotBlank String sheetName,
            String plantCode,
            String currencyCode,
            LocalDate validFrom,
            LocalDate validTo,
            String description
    ) {}

    public record CreateRuleRequest(
            @NotNull Integer sequenceNo,
            @NotBlank String ruleCode,
            @NotBlank String ruleName,
            @NotNull CostingSheetRuleType ruleType,
            CostingSheetBaseCode baseCode,
            BigDecimal percentRate,
            BigDecimal fixedAmount,
            @NotBlank String componentCode,
            String costBehavior
    ) {}

    public record ApplyRequest(
            @NotBlank String sheetId,
            @NotNull BigDecimal material,
            @NotNull BigDecimal process,
            BigDecimal tooling,
            BigDecimal packaging
    ) {}

    public record AppliedLineResponse(
            int sequenceNo,
            String ruleCode,
            String ruleName,
            String ruleType,
            String baseCode,
            BigDecimal baseAmount,
            BigDecimal rateOrFixed,
            BigDecimal amount,
            String componentCode,
            String costBehavior,
            String calculationRule
    ) {}

    public record ApplyResponse(
            String sheetId,
            String sheetCode,
            List<AppliedLineResponse> lines,
            BigDecimal baseSubtotal,
            BigDecimal totalOverhead,
            BigDecimal grandTotal,
            String calculationBasis
    ) {}
}
