package com.automotive.costing.shouldcost.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Should-cost assembly calculation core (M08: SC-01, SC-02). Unlike
 * {@code MaterialCostEngine}/{@code ProcessCostEngine}/{@code ToolingCostEngine}, this
 * engine does not compute a cost element from first principles - it assembles
 * already-computed elements (from M03/M04/M06, plus overhead and SG&amp;A/margin) into
 * the SRS Appendix D nine-element total. Kept as a plain, framework-independent class for
 * the same reason as the other engines: unit-testable against hand-calculated control
 * values, reusable identically from interactive and batch paths.
 *
 * Formulas implemented, per SRS Section 4.8:
 *  - Overhead absorption (feeds SC-01, not a lettered requirement itself but the missing
 *    link OHD-03's absorption rate needed): overheadCost = allocationBaseQuantity x
 *    absorptionRate. The allocation base (SRS 4.5.3 lists direct labor hours, machine
 *    hours, or material cost as options) is a per-BOM-node decision made by the caller
 *    ({@code ShouldCostService}) - this method just multiplies whatever quantity and rate
 *    it's given.
 *  - SC-01: subtotal = material + process + tooling + packaging/freight + overhead;
 *    sgaCost = subtotal x sgaPct; profitMargin = subtotal x marginPct; total = subtotal +
 *    sgaCost + profitMargin. Both SG&amp;A and margin are applied against the same
 *    pre-SG&amp;A/margin subtotal (not compounded on each other) - a documented
 *    assumption, since the SRS specifies that both are "configurable by commodity family
 *    and region" but not the exact compounding order; flagged alongside this project's
 *    other Appendix-B-style open items for client confirmation.
 *  - SC-02: rapid parametric cost = netWeightKg x costPerKg benchmark. Deliberately
 *    bypasses every other formula in this system - the whole point is a fast estimate
 *    before a BOM/routing exists.
 */
public final class ShouldCostEngine {

    private static final int SCALE = 6;

    private ShouldCostEngine() {
    }

    public record FullBuildUpInput(
            BigDecimal materialCost,
            BigDecimal processCost,
            BigDecimal toolingCost,
            BigDecimal packagingFreightCost, // always zero until M07 exists - see ShouldCostBuildUp's Javadoc
            BigDecimal overheadCost,
            BigDecimal sgaPct,
            BigDecimal marginPct
    ) {
    }

    public record FullBuildUpResult(
            BigDecimal subtotal,
            BigDecimal sgaCost,
            BigDecimal profitMargin,
            BigDecimal totalCost,
            String calculationBasis
    ) {
    }

    public static FullBuildUpResult calculateFullBuildUp(FullBuildUpInput input) {
        BigDecimal subtotal = nz(input.materialCost()).add(nz(input.processCost()))
                .add(nz(input.toolingCost())).add(nz(input.packagingFreightCost()))
                .add(nz(input.overheadCost()));

        BigDecimal sgaPct = requireNonNegative(input.sgaPct(), "sgaPct");
        BigDecimal marginPct = requireNonNegative(input.marginPct(), "marginPct");

        BigDecimal sgaCost = subtotal.multiply(sgaPct).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal profitMargin = subtotal.multiply(marginPct).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(sgaCost).add(profitMargin).setScale(SCALE, RoundingMode.HALF_UP);

        String basis = "subtotal[material(" + nz(input.materialCost()).toPlainString() + ") + process("
                + nz(input.processCost()).toPlainString() + ") + tooling(" + nz(input.toolingCost()).toPlainString()
                + ") + packaging/freight(" + nz(input.packagingFreightCost()).toPlainString() + ") + overhead("
                + nz(input.overheadCost()).toPlainString() + ") = " + subtotal.toPlainString()
                + "] + sga(" + subtotal.toPlainString() + " x " + sgaPct.toPlainString() + " = "
                + sgaCost.toPlainString() + ") + margin(" + subtotal.toPlainString() + " x "
                + marginPct.toPlainString() + " = " + profitMargin.toPlainString() + ")";

        return new FullBuildUpResult(subtotal.setScale(SCALE, RoundingMode.HALF_UP), sgaCost, profitMargin,
                total, basis);
    }

    /** Overhead absorption: allocationBaseQuantity x absorptionRate. Feeds calculateFullBuildUp's overheadCost input. */
    public static BigDecimal calculateOverheadCost(BigDecimal allocationBaseQuantity, BigDecimal absorptionRate) {
        if (allocationBaseQuantity == null || allocationBaseQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException(
                    "allocationBaseQuantity must be zero or greater.", "SC-01-overhead-allocation");
        }
        return allocationBaseQuantity.multiply(nz(absorptionRate)).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** SC-02: rapid parametric cost = weight x a $/kg benchmark. */
    public static BigDecimal calculateParametricCost(BigDecimal netWeightKg, BigDecimal costPerKg) {
        if (netWeightKg == null || netWeightKg.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException("netWeightKg must be greater than zero.", "SC-02");
        }
        if (costPerKg == null || costPerKg.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException("costPerKg must be zero or greater.", "SC-02");
        }
        return netWeightKg.multiply(costPerKg).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException(field + " must be zero or greater.", "SC-01");
        }
        return value;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
