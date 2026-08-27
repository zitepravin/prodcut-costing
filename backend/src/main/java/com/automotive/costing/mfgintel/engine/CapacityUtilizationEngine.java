package com.automotive.costing.mfgintel.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** D2 pure capacity math — effective hours, load, utilization, residual. */
public final class CapacityUtilizationEngine {

    private static final int SCALE = 6;

    private CapacityUtilizationEngine() {}

    public record UtilizationResult(
            BigDecimal availableHours,
            BigDecimal efficiencyPct,
            BigDecimal effectiveHours,
            BigDecimal requiredHours,
            BigDecimal residualHours,
            BigDecimal utilizationPct,
            String status // OK | OVERLOADED | NO_CAPACITY
    ) {}

    public static UtilizationResult calculate(
            BigDecimal availableHours,
            BigDecimal efficiencyPct,
            List<BigDecimal> requiredHoursList) {

        BigDecimal available = nz(availableHours);
        BigDecimal eff = efficiencyPct == null || efficiencyPct.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ONE : efficiencyPct;
        BigDecimal effective = available.multiply(eff).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal required = requiredHoursList == null ? BigDecimal.ZERO
                : requiredHoursList.stream().map(CapacityUtilizationEngine::nz)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal residual = effective.subtract(required).setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal utilPct;
        String status;
        if (effective.compareTo(BigDecimal.ZERO) <= 0) {
            utilPct = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
            status = "NO_CAPACITY";
        } else {
            utilPct = required.multiply(new BigDecimal("100"))
                    .divide(effective, SCALE, RoundingMode.HALF_UP);
            status = residual.compareTo(BigDecimal.ZERO) < 0 ? "OVERLOADED" : "OK";
        }
        return new UtilizationResult(available, eff, effective, required, residual, utilPct, status);
    }

    /** D1 activity cost = quantity × rate. */
    public static BigDecimal activityCost(BigDecimal quantity, BigDecimal rate) {
        return nz(quantity).multiply(nz(rate)).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
