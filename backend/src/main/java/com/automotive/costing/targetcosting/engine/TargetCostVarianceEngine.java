package com.automotive.costing.targetcosting.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Target vs actual variance arithmetic (Target Costing).
 * absoluteVariance = actual − target (positive = over target).
 * percentageVariance = absolute / target.
 * exceedsThreshold when |percentage| &gt; thresholdPct (fraction, e.g. 0.05 = 5%).
 */
public final class TargetCostVarianceEngine {

    private static final int SCALE = 6;

    private TargetCostVarianceEngine() {
    }

    public record VarianceInput(
            BigDecimal targetAmount,
            BigDecimal actualAmount,
            BigDecimal thresholdPct // nullable — when null, exceedsThreshold is always false
    ) {
    }

    public record VarianceResult(
            BigDecimal absoluteVariance,
            BigDecimal percentageVariance,
            boolean exceedsThreshold,
            String calculationBasis
    ) {
    }

    public static VarianceResult calculate(VarianceInput input) {
        BigDecimal target = requirePositive(input.targetAmount(), "targetAmount");
        BigDecimal actual = requireNonNegative(input.actualAmount(), "actualAmount");

        BigDecimal absolute = actual.subtract(target).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal percentage = absolute.divide(target, SCALE, RoundingMode.HALF_UP);

        boolean exceeds = false;
        if (input.thresholdPct() != null) {
            BigDecimal threshold = input.thresholdPct();
            if (threshold.compareTo(BigDecimal.ZERO) < 0 || threshold.compareTo(BigDecimal.ONE) >= 0) {
                throw new BusinessRuleViolationException(
                        "thresholdPct must be a fraction in [0, 1) - got " + threshold, "TC-01");
            }
            exceeds = percentage.abs().compareTo(threshold) > 0;
        }

        String basis = "actual(" + actual.toPlainString() + ") - target(" + target.toPlainString()
                + ") = abs " + absolute.toPlainString()
                + " (" + percentage.toPlainString() + "); exceedsThreshold=" + exceeds
                + (input.thresholdPct() != null ? " @ " + input.thresholdPct().toPlainString() : "");

        return new VarianceResult(absolute, percentage, exceeds, basis);
    }

    private static BigDecimal requirePositive(BigDecimal v, String field) {
        if (v == null || v.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException(field + " must be > 0", "TC-02");
        }
        return v;
    }

    private static BigDecimal requireNonNegative(BigDecimal v, String field) {
        if (v == null || v.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException(field + " must be >= 0", "TC-03");
        }
        return v;
    }
}
