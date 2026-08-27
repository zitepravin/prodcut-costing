package com.automotive.costing.tooling.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.tooling.entity.ToolingOwnership;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Tooling amortization calculation core (M06: TL-02, TL-04, TL-05). Same rationale as
 * {@code MaterialCostEngine}/{@code ProcessCostEngine}: a plain, framework-independent
 * Java class, unit-tested against hand-calculated control values.
 *
 * Formulas implemented, per SRS Section 4.6:
 *  - TL-02: base amortized cost per piece = toolCost / programLifetimeVolume. SRS 4.6.5's
 *    hard business rule ("Tooling amortization per piece cannot be calculated without a
 *    defined program lifetime volume; the system must block calculation and prompt for
 *    this value") is enforced as an explicit guard, not a silent divide-by-zero.
 *  - TL-04: if a tool-life limit (toolLifeHits) is defined and the program's lifetime
 *    volume exceeds it, refurbishment is triggered automatically (SRS 4.6.5's second
 *    business rule): refurbishmentEvents = ceil(lifetimeVolume / toolLifeHits) - 1, and
 *    that count's cost is amortized across the same lifetime volume as the base cost.
 *  - TL-05: {@code includedInPiecePrice} carries the ownership rule (SRS 4.6.5:
 *    "Customer-owned tooling cost is excluded from supplier piece-price but tracked
 *    separately as a capital line item") forward into the result, rather than zeroing the
 *    cost fields outright - the amortized figures remain calculated and visible (for the
 *    "tracked separately as a capital line item" half of the rule), only the
 *    piece-price-inclusion flag changes.
 */
public final class ToolingCostEngine {

    private static final int SCALE = 6;

    private ToolingCostEngine() {
    }

    public record ToolingCostInput(
            BigDecimal toolCost,
            BigDecimal programLifetimeVolume,
            BigDecimal toolLifeHits,               // nullable - no defined limit if absent
            BigDecimal refurbishmentCostPerEvent,   // nullable - treated as zero if refurbishment is otherwise triggered
            ToolingOwnership ownership
    ) {
    }

    public record ToolingCostResult(
            BigDecimal baseAmortizedCostPerPiece,
            int refurbishmentEventsProjected,
            BigDecimal refurbishmentCostPerPiece,
            BigDecimal totalAmortizedCostPerPiece,
            boolean includedInPiecePrice,
            String calculationBasis
    ) {
    }

    public static ToolingCostResult calculate(ToolingCostInput input) {
        BigDecimal lifetimeVolume = requirePositiveLifetimeVolume(input.programLifetimeVolume());
        BigDecimal toolCost = requirePositive(input.toolCost(), "toolCost");

        BigDecimal baseAmortized = toolCost.divide(lifetimeVolume, SCALE, RoundingMode.HALF_UP);

        int refurbishmentEvents = 0;
        BigDecimal refurbishmentCostPerPiece = BigDecimal.ZERO;
        if (input.toolLifeHits() != null && input.toolLifeHits().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal lifeCycles = lifetimeVolume.divide(input.toolLifeHits(), 10, RoundingMode.HALF_UP)
                    .setScale(0, RoundingMode.CEILING);
            refurbishmentEvents = Math.max(0, lifeCycles.intValue() - 1);
            if (refurbishmentEvents > 0) {
                BigDecimal costPerEvent = input.refurbishmentCostPerEvent() == null
                        ? BigDecimal.ZERO : input.refurbishmentCostPerEvent();
                BigDecimal totalRefurbishmentCost = costPerEvent.multiply(BigDecimal.valueOf(refurbishmentEvents));
                refurbishmentCostPerPiece = totalRefurbishmentCost.divide(lifetimeVolume, SCALE, RoundingMode.HALF_UP);
            }
        }

        BigDecimal total = baseAmortized.add(refurbishmentCostPerPiece).setScale(SCALE, RoundingMode.HALF_UP);
        boolean includedInPiecePrice = input.ownership() != ToolingOwnership.CUSTOMER;

        String basis = "base[" + toolCost.toPlainString() + " / " + lifetimeVolume.toPlainString() + " = "
                + baseAmortized.toPlainString() + "] + refurbishment[" + refurbishmentEvents + " event(s) = "
                + refurbishmentCostPerPiece.toPlainString() + "], ownership=" + input.ownership()
                + " (includedInPiecePrice=" + includedInPiecePrice + ")";

        return new ToolingCostResult(baseAmortized, refurbishmentEvents, refurbishmentCostPerPiece, total,
                includedInPiecePrice, basis);
    }

    /** SRS 4.6.5: block calculation entirely (not a silent zero/error-later) when lifetime volume is undefined. */
    private static BigDecimal requirePositiveLifetimeVolume(BigDecimal lifetimeVolume) {
        if (lifetimeVolume == null || lifetimeVolume.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException(
                    "Tooling amortization cannot be calculated without a defined, positive program lifetime "
                            + "volume. Set the vehicle program's lifetimeVolume before calculating.",
                    "SRS-4.6.5-lifetime-volume-required");
        }
        return lifetimeVolume;
    }

    private static BigDecimal requirePositive(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException(field + " must be greater than zero.", "TL-02");
        }
        return value;
    }
}
