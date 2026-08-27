package com.automotive.costing.process.engine;

import com.automotive.costing.common.costing.CostComponentResult;
import com.automotive.costing.common.exception.BusinessRuleViolationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Power cost calculator (V277).
 * Power Cost = Machine kW × Runtime Hours × Electricity Rate
 */
public final class PowerCostEngine {

    public static final String COMPONENT_CODE = "POWER";
    public static final String FORMULA_CODE = "PWR-01";
    private static final int SCALE = 6;

    private PowerCostEngine() {
    }

    public record PowerCostInput(
            String operationId,
            BigDecimal machineKw,
            BigDecimal runtimeHours,
            BigDecimal electricityRate,
            String rateCurrency,
            String rateSource,
            LocalDate rateEffectiveDate,
            LocalDate costingDate,
            String bomNodeId
    ) {
    }

    /**
     * Calculate power cost. Missing mandatory inputs throw; valid zero values are allowed.
     */
    public static CostComponentResult calculate(PowerCostInput input) {
        if (input.machineKw() == null) {
            throw new BusinessRuleViolationException(
                    "Machine kW is required for power costing", "PWR-MISSING-KW");
        }
        if (input.machineKw().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException(
                    "Machine kW must be >= 0", "PWR-NEG-KW");
        }
        if (input.runtimeHours() == null) {
            throw new BusinessRuleViolationException(
                    "Runtime hours is required for power costing", "PWR-MISSING-RUNTIME");
        }
        if (input.runtimeHours().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException(
                    "Runtime hours must be >= 0", "PWR-NEG-RUNTIME");
        }
        if (input.electricityRate() == null) {
            throw new BusinessRuleViolationException(
                    "Electricity rate is required for power costing", "PWR-MISSING-RATE");
        }
        if (input.electricityRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException(
                    "Electricity rate must be >= 0", "PWR-NEG-RATE");
        }

        // amount = kW * hours * rate
        BigDecimal amount = input.machineKw()
                .multiply(input.runtimeHours())
                .multiply(input.electricityRate())
                .setScale(SCALE, RoundingMode.HALF_UP);

        String ccy = input.rateCurrency() != null ? input.rateCurrency() : "USD";
        String explanation = "Power = " + input.machineKw().toPlainString() + " kW × "
                + input.runtimeHours().toPlainString() + " h × "
                + input.electricityRate().toPlainString() + " " + ccy + "/kWh"
                + " = " + amount.toPlainString() + " " + ccy;

        return CostComponentResult.builder(COMPONENT_CODE)
                .quantity(input.machineKw().multiply(input.runtimeHours()).setScale(SCALE, RoundingMode.HALF_UP))
                .uom("kWh")
                .rate(input.electricityRate())
                .rateCurrency(ccy)
                .amount(amount)
                .currency(ccy)
                .rateSource(input.rateSource())
                .rateEffectiveDate(input.rateEffectiveDate())
                .formulaCode(FORMULA_CODE)
                .explanation(explanation)
                .sourceReference("machineKw*runtimeHours*electricityRate")
                .bomNodeId(input.bomNodeId())
                .operationId(input.operationId())
                .build();
    }
}
