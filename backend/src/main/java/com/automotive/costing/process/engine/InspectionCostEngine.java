package com.automotive.costing.process.engine;

import com.automotive.costing.common.costing.CostComponentResult;
import com.automotive.costing.common.exception.BusinessRuleViolationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Inspection cost calculator (V277).
 * Base: Inspection Time (hours) × Inspector Rate
 * Optional fixed charge per activity.
 */
public final class InspectionCostEngine {

    public static final String COMPONENT_CODE = "INSPECTION";
    public static final String FORMULA_CODE = "INSP-01";
    private static final int SCALE = 6;

    private InspectionCostEngine() {
    }

    public record InspectionActivity(
            String activityId,
            BigDecimal inspectionTimeHours,
            BigDecimal inspectorRate,
            String rateCurrency,
            String rateSource,
            LocalDate rateEffectiveDate,
            BigDecimal fixedCharge
    ) {
    }

    public record InspectionCostInput(
            String operationId,
            String bomNodeId,
            List<InspectionActivity> activities,
            LocalDate costingDate
    ) {
    }

    public static List<CostComponentResult> calculate(InspectionCostInput input) {
        if (input.activities() == null || input.activities().isEmpty()) {
            return List.of();
        }
        List<CostComponentResult> results = new ArrayList<>();
        for (InspectionActivity a : input.activities()) {
            results.add(calculateActivity(input, a));
        }
        return results;
    }

    private static CostComponentResult calculateActivity(InspectionCostInput input, InspectionActivity a) {
        if (a.inspectionTimeHours() == null) {
            throw new BusinessRuleViolationException(
                    "Inspection time is required", "INSP-MISSING-TIME");
        }
        if (a.inspectionTimeHours().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException(
                    "Inspection time must be >= 0", "INSP-NEG-TIME");
        }
        if (a.inspectorRate() == null) {
            throw new BusinessRuleViolationException(
                    "Inspector rate is required", "INSP-MISSING-RATE");
        }
        if (a.inspectorRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException(
                    "Inspector rate must be >= 0", "INSP-NEG-RATE");
        }
        BigDecimal fixed = a.fixedCharge() == null ? BigDecimal.ZERO : a.fixedCharge();
        if (fixed.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException(
                    "Fixed inspection charge must be >= 0", "INSP-NEG-FIXED");
        }

        BigDecimal variable = a.inspectionTimeHours().multiply(a.inspectorRate());
        BigDecimal amount = variable.add(fixed).setScale(SCALE, RoundingMode.HALF_UP);
        String ccy = a.rateCurrency() != null ? a.rateCurrency() : "USD";
        String explanation = "Inspection = " + a.inspectionTimeHours().toPlainString() + " h × "
                + a.inspectorRate().toPlainString() + " + fixed " + fixed.toPlainString()
                + " = " + amount.toPlainString() + " " + ccy;

        return CostComponentResult.builder(COMPONENT_CODE)
                .quantity(a.inspectionTimeHours())
                .uom("h")
                .rate(a.inspectorRate())
                .rateCurrency(ccy)
                .amount(amount)
                .currency(ccy)
                .rateSource(a.rateSource())
                .rateEffectiveDate(a.rateEffectiveDate())
                .formulaCode(FORMULA_CODE)
                .explanation(explanation)
                .sourceReference(a.activityId())
                .bomNodeId(input.bomNodeId())
                .operationId(input.operationId())
                .build();
    }
}
