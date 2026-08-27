package com.automotive.costing.material.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.material.engine.MaterialCostEngine.MaterialCostInput;
import com.automotive.costing.material.engine.MaterialCostEngine.MaterialCostResult;
import com.automotive.costing.material.entity.PricingBasis;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for MaterialCostEngine, mapped to QA_Test_Case_Tracker.xlsx Module M03
 * (MAT-01, MAT-02, MAT-03, MAT-06). Every expected value here is hand-calculated (values
 * cross-checked independently with Python's Decimal against BigDecimal HALF_UP semantics,
 * per Test Plan Section 12 risk #1 / TDD Section 6.4's "known, hand-calculated input/output
 * pairs" mandate) - never asserted against the engine's own output.
 */
class MaterialCostEngineTest {

    // TC-0033 (MAT-01, Positive) + gross-cost/net-of-scrap-credit control calculation:
    // netWeight=2.5kg, scrap=10%, rate=$100/kg, scrap recovery=$20/kg.
    // grossWeight = 2.5 / 0.9 = 2.7777777778 (scale 10, HALF_UP)
    // scrapRecoveryCredit = 20 x (2.7777777778 - 2.5) = 5.555556 (rounded for display)
    // materialCost = (2.7777777778 x 100) - 5.55555556.. = 272.222222 (scale 6, HALF_UP)
    @Test
    void calculate_withScrapAndRecoveryCredit_matchesHandCalculatedControlValue() {
        MaterialCostResult result = MaterialCostEngine.calculate(new MaterialCostInput(
                new BigDecimal("2.500000"), new BigDecimal("0.100000"), new BigDecimal("100.000000"),
                PricingBasis.FIXED, new BigDecimal("20.000000"), "sheet"));

        assertThat(result.grossWeight()).isEqualByComparingTo("2.777778");
        assertThat(result.scrapRecoveryCredit()).isEqualByComparingTo("5.555556");
        assertThat(result.materialCost()).isEqualByComparingTo("272.222222");
    }

    // TC-0034 (MAT-01, Negative/Boundary): 100% scrap must not silently divide by zero.
    @Test
    void calculate_withScrapPctAtOneHundredPercent_throwsBusinessRuleViolation() {
        MaterialCostInput input = new MaterialCostInput(
                new BigDecimal("2.500000"), BigDecimal.ONE, new BigDecimal("100.000000"),
                PricingBasis.FIXED, null, "sheet");

        assertThatThrownBy(() -> MaterialCostEngine.calculate(input))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("divide-by-zero");
    }

    // TC-0034 boundary variant: a negative scrapPct is equally invalid.
    @Test
    void calculate_withNegativeScrapPct_throwsBusinessRuleViolation() {
        MaterialCostInput input = new MaterialCostInput(
                new BigDecimal("2.500000"), new BigDecimal("-0.05"), new BigDecimal("100.000000"),
                PricingBasis.FIXED, null, "sheet");

        assertThatThrownBy(() -> MaterialCostEngine.calculate(input))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    // MAT-01: null scrapPct is a legitimate "no scrap allowance" input, not an error -
    // gross weight equals net weight.
    @Test
    void calculate_withNullScrapPct_treatsGrossWeightAsNetWeight() {
        MaterialCostResult result = MaterialCostEngine.calculate(new MaterialCostInput(
                new BigDecimal("2.500000"), null, new BigDecimal("100.000000"),
                PricingBasis.FIXED, null, "sheet"));

        assertThat(result.grossWeight()).isEqualByComparingTo("2.500000");
        assertThat(result.scrapRecoveryCredit()).isEqualByComparingTo("0.000000");
        assertThat(result.materialCost()).isEqualByComparingTo("250.000000");
    }

    // TC-0035 (MAT-02, Positive): index-linked rate = base + index + alloy surcharge.
    @Test
    void computeIndexLinkedRate_sumsBasePriceIndexAndSurcharge() {
        BigDecimal rate = MaterialCostEngine.computeIndexLinkedRate(
                new BigDecimal("1.200000"), new BigDecimal("0.350000"), new BigDecimal("0.075000"));

        assertThat(rate).isEqualByComparingTo("1.625000");
    }

    // TC-0036 (MAT-02, Negative/Boundary): index-linked pricing without a resolvable index
    // value must be rejected explicitly, not silently default to zero.
    @Test
    void computeIndexLinkedRate_withoutIndexValue_throwsBusinessRuleViolation() {
        assertThatThrownBy(() -> MaterialCostEngine.computeIndexLinkedRate(
                new BigDecimal("1.200000"), null, new BigDecimal("0.075000")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("MAT-02");
    }

    @Test
    void computeIndexLinkedRate_withoutBasePrice_throwsBusinessRuleViolation() {
        assertThatThrownBy(() -> MaterialCostEngine.computeIndexLinkedRate(
                null, new BigDecimal("0.350000"), new BigDecimal("0.075000")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    // TC-0037 (MAT-03, Positive): no scrap recovery rate defined -> zero credit, not an error.
    @Test
    void calculate_withoutScrapRecoveryRate_appliesZeroCredit() {
        MaterialCostResult result = MaterialCostEngine.calculate(new MaterialCostInput(
                new BigDecimal("2.500000"), new BigDecimal("0.100000"), new BigDecimal("100.000000"),
                PricingBasis.FIXED, null, "sheet"));

        assertThat(result.scrapRecoveryCredit()).isEqualByComparingTo("0.000000");
    }

    // TC-0043 (MAT-06, Positive): the engine accepts every one of the 8 SRS-listed forms
    // without error, and (absent a client-confirmed per-form coefficient) produces the
    // same base formula result for all of them - the documented, non-invented MVP behavior.
    @Test
    void calculate_acrossAllEightMaterialForms_appliesNoUnconfirmedFormAdjustment() {
        String[] forms = {"sheet", "coil", "bar", "billet", "casting", "forging_blank",
                "resin_pellet", "rubber_compound"};

        for (String form : forms) {
            MaterialCostResult result = MaterialCostEngine.calculate(new MaterialCostInput(
                    new BigDecimal("2.500000"), null, new BigDecimal("100.000000"),
                    PricingBasis.FIXED, null, form));
            assertThat(result.materialCost()).isEqualByComparingTo("250.000000");
        }
    }

    // MAT-06 extension point: a FormCostAdjuster, once the client confirms per-form
    // coefficients, plugs in without changing calculate()'s call sites.
    @Test
    void calculate_withCustomFormCostAdjuster_appliesTheAdjustment() {
        MaterialCostEngine.FormCostAdjuster tenPercentSurcharge =
                (cost, form) -> cost.multiply(new BigDecimal("1.10"));

        MaterialCostResult result = MaterialCostEngine.calculate(new MaterialCostInput(
                new BigDecimal("2.500000"), null, new BigDecimal("100.000000"),
                PricingBasis.FIXED, null, "casting"), tenPercentSurcharge);

        assertThat(result.materialCost()).isEqualByComparingTo("275.000000");
    }

    @Test
    void calculate_withZeroNetWeight_throwsBusinessRuleViolation() {
        MaterialCostInput input = new MaterialCostInput(
                BigDecimal.ZERO, new BigDecimal("0.1"), new BigDecimal("100.000000"),
                PricingBasis.FIXED, null, "sheet");

        assertThatThrownBy(() -> MaterialCostEngine.calculate(input))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void calculate_withNegativeUnitRate_throwsBusinessRuleViolation() {
        MaterialCostInput input = new MaterialCostInput(
                new BigDecimal("2.500000"), new BigDecimal("0.1"), new BigDecimal("-1.000000"),
                PricingBasis.FIXED, null, "sheet");

        assertThatThrownBy(() -> MaterialCostEngine.calculate(input))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
