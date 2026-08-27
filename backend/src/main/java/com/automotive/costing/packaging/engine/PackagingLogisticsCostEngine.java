package com.automotive.costing.packaging.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.packaging.entity.FreightMode;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Packaging & Logistics cost calculation core (M07).
 * Framework-independent pure arithmetic — same pattern as MaterialCostEngine / ToolingCostEngine.
 *
 * <p>Formulas (piece-level):
 * <ul>
 *   <li>packaging = primaryPack + secondaryPack
 *       + (returnableContainerCost / containerLifeCycles) when life cycles &gt; 0</li>
 *   <li>freight = directionMultiplier × (fixedFreightPerUnit + netWeightKg × freightRatePerKg)
 *       where directionMultiplier is 0 (NONE), 1 (INBOUND or OUTBOUND), or 2 (BOTH)</li>
 *   <li>duty = (packaging + freight) × dutyPct when dutyPct is in [0, 1)</li>
 *   <li>total = packaging + freight + duty</li>
 * </ul>
 *
 * All money values use {@link BigDecimal} with scale 6, HALF_UP.
 */
public final class PackagingLogisticsCostEngine {

    private static final int SCALE = 6;

    private PackagingLogisticsCostEngine() {
    }

    public record PackagingLogisticsCostInput(
            BigDecimal primaryPackCostPerPiece,
            BigDecimal secondaryPackCostPerPiece,
            BigDecimal returnableContainerCost,   // nullable
            BigDecimal containerLifeCycles,       // nullable
            BigDecimal netWeightKg,                // nullable — required only if freightRatePerKg set
            FreightMode freightMode,
            BigDecimal freightRatePerKg,          // nullable
            BigDecimal fixedFreightPerUnit,       // nullable
            BigDecimal dutyPct                    // nullable, fraction [0, 1)
    ) {
    }

    public record PackagingLogisticsCostResult(
            BigDecimal packagingCostPerPiece,
            BigDecimal freightCostPerPiece,
            BigDecimal dutyCostPerPiece,
            BigDecimal totalPackagingFreightCost,
            String calculationBasis
    ) {
    }

    public static PackagingLogisticsCostResult calculate(PackagingLogisticsCostInput input) {
        BigDecimal primary = nz(input.primaryPackCostPerPiece());
        BigDecimal secondary = nz(input.secondaryPackCostPerPiece());
        requireNonNegative(primary, "primaryPackCostPerPiece");
        requireNonNegative(secondary, "secondaryPackCostPerPiece");

        BigDecimal returnableAmortized = BigDecimal.ZERO;
        if (input.returnableContainerCost() != null
                && input.returnableContainerCost().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal lifeCycles = input.containerLifeCycles();
            if (lifeCycles == null || lifeCycles.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleViolationException(
                        "containerLifeCycles must be > 0 when returnableContainerCost is set",
                        "PKG-01");
            }
            returnableAmortized = input.returnableContainerCost()
                    .divide(lifeCycles, SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal packaging = primary.add(secondary).add(returnableAmortized)
                .setScale(SCALE, RoundingMode.HALF_UP);

        FreightMode mode = input.freightMode() == null ? FreightMode.NONE : input.freightMode();
        int directionMultiplier = switch (mode) {
            case NONE -> 0;
            case INBOUND, OUTBOUND -> 1;
            case BOTH -> 2;
        };

        BigDecimal unitFreight = BigDecimal.ZERO;
        if (directionMultiplier > 0) {
            BigDecimal fixed = nz(input.fixedFreightPerUnit());
            requireNonNegative(fixed, "fixedFreightPerUnit");
            BigDecimal weightRate = BigDecimal.ZERO;
            if (input.freightRatePerKg() != null
                    && input.freightRatePerKg().compareTo(BigDecimal.ZERO) > 0) {
                requireNonNegative(input.freightRatePerKg(), "freightRatePerKg");
                BigDecimal weight = input.netWeightKg();
                if (weight == null || weight.compareTo(BigDecimal.ZERO) < 0) {
                    throw new BusinessRuleViolationException(
                            "netWeightKg must be >= 0 when freightRatePerKg is set",
                            "PKG-02");
                }
                weightRate = weight.multiply(input.freightRatePerKg());
            }
            unitFreight = fixed.add(weightRate);
        }

        BigDecimal freight = unitFreight
                .multiply(BigDecimal.valueOf(directionMultiplier))
                .setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal duty = BigDecimal.ZERO;
        if (input.dutyPct() != null && input.dutyPct().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal dutyPct = input.dutyPct();
            if (dutyPct.compareTo(BigDecimal.ONE) >= 0) {
                throw new BusinessRuleViolationException(
                        "dutyPct must be a fraction in [0, 1) - got " + dutyPct,
                        "PKG-03");
            }
            duty = packaging.add(freight).multiply(dutyPct).setScale(SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal total = packaging.add(freight).add(duty).setScale(SCALE, RoundingMode.HALF_UP);

        String basis = "packaging[primary=" + primary.toPlainString()
                + " + secondary=" + secondary.toPlainString()
                + " + returnableAmortized=" + returnableAmortized.toPlainString()
                + " = " + packaging.toPlainString()
                + "] + freight[mode=" + mode
                + " x unit=" + unitFreight.toPlainString()
                + " = " + freight.toPlainString()
                + "] + duty[" + duty.toPlainString()
                + "] = total " + total.toPlainString();

        return new PackagingLogisticsCostResult(packaging, freight, duty, total, basis);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static void requireNonNegative(BigDecimal value, String field) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException(field + " must be >= 0", "PKG-04");
        }
    }
}
