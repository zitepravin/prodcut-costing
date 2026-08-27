package com.automotive.costing.process.engine;

import com.automotive.costing.common.costing.CostComponentResult;
import com.automotive.costing.common.exception.BusinessRuleViolationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Consumable cost calculator (V277).
 * Each consumable: Quantity × Unit Rate
 */
public final class ConsumableCostEngine {

    public static final String FORMULA_CODE = "CON-01";
    private static final int SCALE = 6;

    public enum ConsumableType {
        WIRE, GAS, COOLANT, ABRASIVE, LUBRICANT, OTHER_CONSUMABLE
    }

    private ConsumableCostEngine() {
    }

    public record ConsumableLine(
            ConsumableType type,
            BigDecimal quantity,
            String uom,
            BigDecimal unitRate,
            String rateCurrency,
            String rateSource,
            LocalDate rateEffectiveDate
    ) {
    }

    public record ConsumableCostInput(
            String operationId,
            String bomNodeId,
            List<ConsumableLine> lines,
            LocalDate costingDate
    ) {
    }

    public static List<CostComponentResult> calculate(ConsumableCostInput input) {
        if (input.lines() == null || input.lines().isEmpty()) {
            return List.of();
        }
        List<CostComponentResult> results = new ArrayList<>();
        for (ConsumableLine line : input.lines()) {
            results.add(calculateLine(input, line));
        }
        return results;
    }

    private static CostComponentResult calculateLine(ConsumableCostInput input, ConsumableLine line) {
        if (line.type() == null) {
            throw new BusinessRuleViolationException("Consumable type is required", "CON-MISSING-TYPE");
        }
        if (line.quantity() == null) {
            throw new BusinessRuleViolationException(
                    "Quantity required for consumable " + line.type(), "CON-MISSING-QTY");
        }
        if (line.quantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException(
                    "Quantity must be >= 0 for consumable " + line.type(), "CON-NEG-QTY");
        }
        if (line.unitRate() == null) {
            throw new BusinessRuleViolationException(
                    "Unit rate required for consumable " + line.type(), "CON-MISSING-RATE");
        }
        if (line.unitRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException(
                    "Unit rate must be >= 0 for consumable " + line.type(), "CON-NEG-RATE");
        }

        BigDecimal amount = line.quantity().multiply(line.unitRate()).setScale(SCALE, RoundingMode.HALF_UP);
        String ccy = line.rateCurrency() != null ? line.rateCurrency() : "USD";
        String code = "CONSUMABLE_" + line.type().name();
        String explanation = line.type() + " = " + line.quantity().toPlainString()
                + " × " + line.unitRate().toPlainString() + " = " + amount.toPlainString() + " " + ccy;

        return CostComponentResult.builder(code)
                .quantity(line.quantity())
                .uom(line.uom())
                .rate(line.unitRate())
                .rateCurrency(ccy)
                .amount(amount)
                .currency(ccy)
                .rateSource(line.rateSource())
                .rateEffectiveDate(line.rateEffectiveDate())
                .formulaCode(FORMULA_CODE)
                .explanation(explanation)
                .sourceReference(line.type().name())
                .bomNodeId(input.bomNodeId())
                .operationId(input.operationId())
                .build();
    }
}
