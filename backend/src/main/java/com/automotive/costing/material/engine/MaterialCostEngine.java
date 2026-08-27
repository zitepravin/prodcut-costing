package com.automotive.costing.material.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.material.entity.PricingBasis;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Raw material cost calculation core (M03: MAT-01, MAT-02, MAT-03, MAT-06). Deliberately a
 * plain, framework-independent Java class with no Spring/JPA dependency - same rationale as
 * {@code BomRollupEngine} (TDD Section 6.1): unit-testable in isolation against hand-calculated
 * control values (TDD Section 6.4, Test Plan Section 12 risk #1 "costing calculation accuracy
 * ... a silent calculation error directly misleads multi-million-dollar sourcing decisions"),
 * and reusable identically from an interactive API call or a batch MAT-05 recalculation job.
 *
 * Formulas implemented, per SRS Section 4.3:
 *  - MAT-01: grossWeight = netWeight / (1 - scrapPct). scrapPct is a FRACTION (0 &lt;= x &lt; 1,
 *    e.g. 0.12 for 12%), matching {@code BomNode.scrapPct}'s convention. Guards scrapPct = 1
 *    (100% scrap - would divide by zero) and any out-of-range value, per TDD Section 6.2's
 *    "guard every division against zero/blank denominators with an explicit validation error."
 *  - MAT-02: index-linked unit rate = basePrice + indexValue + alloySurchargePerUnit, used
 *    instead of the material master's flat {@code rate} when pricingBasis = INDEX_LINKED.
 *  - MAT-03: scrapRecoveryCredit = scrapRecoveryRate x (grossWeight - netWeight) - the resale
 *    value of the scrap generated, deducted from gross material cost where a recovery rate is
 *    defined for the grade (SRS 4.3.3 "scrap value recovery credit").
 *  - MAT-06: material form is accepted and carried through to the result for form-specific
 *    downstream reporting/selection (SRS 4.3.4 MAT-06 lists 8 supported forms), but this MVP
 *    does not apply an undocumented per-form cost coefficient - no such formula is specified
 *    anywhere in the SRS/TDD, and CLAUDE.md instructs not to invent business requirements.
 *    {@link FormCostAdjuster} is the explicit, named extension point for when the client
 *    confirms form-specific adjustment factors (tracked as an open item, not a silent gap).
 *
 * Final result: materialCost = (grossWeight x unitRate) - scrapRecoveryCredit.
 */
public final class MaterialCostEngine {

    private static final int SCALE = 6;

    private MaterialCostEngine() {
    }

    /**
     * Optional, pluggable per-form cost adjustment (MAT-06). The default {@link #NONE}
     * applies no adjustment - every supported form costs exactly (gross weight x rate)
     * net of scrap credit, until the client confirms otherwise (SRS Appendix B is the
     * mechanism for that confirmation across this whole project).
     */
    @FunctionalInterface
    public interface FormCostAdjuster {
        BigDecimal adjust(BigDecimal computedCost, String materialForm);

        FormCostAdjuster NONE = (cost, form) -> cost;
    }

    public record MaterialCostInput(
            BigDecimal netWeight,
            BigDecimal scrapPct,
            BigDecimal unitRate,
            PricingBasis pricingBasis,
            BigDecimal scrapRecoveryRate, // nullable - MAT-03 only applies if defined
            String materialForm // nullable - MAT-06
    ) {
    }

    public record MaterialCostResult(
            BigDecimal grossWeight,
            BigDecimal scrapRecoveryCredit,
            BigDecimal materialCost,
            String calculationBasis
    ) {
    }

    public static MaterialCostResult calculate(MaterialCostInput input) {
        return calculate(input, FormCostAdjuster.NONE);
    }

    public static MaterialCostResult calculate(MaterialCostInput input, FormCostAdjuster formCostAdjuster) {
        BigDecimal netWeight = requirePositive(input.netWeight(), "netWeight");
        BigDecimal unitRate = requireNonNegative(input.unitRate(), "unitRate");
        BigDecimal grossWeight = computeGrossWeight(netWeight, input.scrapPct());

        BigDecimal scrapRecoveryCredit = computeScrapRecoveryCredit(
                netWeight, grossWeight, input.scrapRecoveryRate());

        BigDecimal grossCost = grossWeight.multiply(unitRate);
        BigDecimal materialCost = grossCost.subtract(scrapRecoveryCredit);
        materialCost = formCostAdjuster.adjust(materialCost, input.materialForm())
                .setScale(SCALE, RoundingMode.HALF_UP);

        String basis = "grossWeight(" + grossWeight.toPlainString() + ") x unitRate("
                + unitRate.toPlainString() + ") - scrapRecoveryCredit("
                + scrapRecoveryCredit.toPlainString() + ") [" + input.pricingBasis() + "]";

        return new MaterialCostResult(
                grossWeight.setScale(SCALE, RoundingMode.HALF_UP),
                scrapRecoveryCredit.setScale(SCALE, RoundingMode.HALF_UP),
                materialCost,
                basis);
    }

    /**
     * MAT-01: gross weight = net weight / (1 - scrap%). A null scrapPct is treated as zero
     * scrap (gross = net) rather than an error, since not every part has a scrap allowance.
     */
    public static BigDecimal computeGrossWeight(BigDecimal netWeight, BigDecimal scrapPct) {
        if (scrapPct == null) {
            return netWeight;
        }
        if (scrapPct.compareTo(BigDecimal.ZERO) < 0 || scrapPct.compareTo(BigDecimal.ONE) >= 0) {
            throw new BusinessRuleViolationException(
                    "scrapPct must be a fraction in the range [0, 1) - a value of 1 (100% scrap) "
                            + "or greater makes gross weight undefined (divide-by-zero or negative).",
                    "MAT-01-divide-by-zero-guard");
        }
        return netWeight.divide(BigDecimal.ONE.subtract(scrapPct), 10, RoundingMode.HALF_UP);
    }

    /**
     * MAT-02: index-linked unit rate = base price + commodity index value (as of the
     * calculation's effective date) + alloy surcharge per unit. Caller resolves the
     * index value itself (CommodityIndexValueRepository) so this class stays free of any
     * persistence dependency, consistent with BomRollupEngine's design.
     */
    public static BigDecimal computeIndexLinkedRate(BigDecimal basePrice, BigDecimal indexValue,
                                                      BigDecimal alloySurchargePerUnit) {
        if (basePrice == null) {
            throw new BusinessRuleViolationException(
                    "basePrice is required for index-linked pricing (MAT-02).", "MAT-02");
        }
        if (indexValue == null) {
            throw new BusinessRuleViolationException(
                    "No commodity index value is available as of the requested effective date "
                            + "(MAT-02) - publish a CommodityIndexValue before calculating.", "MAT-02");
        }
        return basePrice.add(indexValue).add(nz(alloySurchargePerUnit))
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** MAT-03: only applies where a scrap recovery rate is actually defined for the grade. */
    private static BigDecimal computeScrapRecoveryCredit(BigDecimal netWeight, BigDecimal grossWeight,
                                                           BigDecimal scrapRecoveryRate) {
        if (scrapRecoveryRate == null || scrapRecoveryRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal scrapWeight = grossWeight.subtract(netWeight);
        return scrapRecoveryRate.multiply(scrapWeight);
    }

    private static BigDecimal requirePositive(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException(
                    field + " must be greater than zero.", "MAT-01");
        }
        return value;
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException(
                    field + " must be present and >= zero.", "MAT-02");
        }
        return value;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
