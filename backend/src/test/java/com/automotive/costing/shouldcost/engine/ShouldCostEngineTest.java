package com.automotive.costing.shouldcost.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.shouldcost.engine.ShouldCostEngine.FullBuildUpInput;
import com.automotive.costing.shouldcost.engine.ShouldCostEngine.FullBuildUpResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ShouldCostEngine, mapped to QA_Test_Case_Tracker.xlsx Module M08
 * (SC-01, SC-02). Expected values hand-calculated exactly (all inputs are terminating
 * decimals, no rounding ambiguity to cross-check against Python for).
 */
class ShouldCostEngineTest {

    // TC-0101 (SC-01, Positive): subtotal=100+50+20+0+30=200; sga=200x0.10=20; margin=200x0.08=16; total=236.
    @Test
    void calculateFullBuildUp_matchesHandCalculatedControlValue() {
        FullBuildUpResult result = ShouldCostEngine.calculateFullBuildUp(new FullBuildUpInput(
                new BigDecimal("100.000000"), new BigDecimal("50.000000"), new BigDecimal("20.000000"),
                BigDecimal.ZERO, new BigDecimal("30.000000"), new BigDecimal("0.10"), new BigDecimal("0.08")));

        assertThat(result.subtotal()).isEqualByComparingTo("200.000000");
        assertThat(result.sgaCost()).isEqualByComparingTo("20.000000");
        assertThat(result.profitMargin()).isEqualByComparingTo("16.000000");
        assertThat(result.totalCost()).isEqualByComparingTo("236.000000");
    }

    // SC-01: any element may be null (not yet calculated) and is treated as zero, not an error.
    @Test
    void calculateFullBuildUp_withMissingElements_treatsThemAsZero() {
        FullBuildUpResult result = ShouldCostEngine.calculateFullBuildUp(new FullBuildUpInput(
                new BigDecimal("100.000000"), null, null, null, null,
                new BigDecimal("0.10"), new BigDecimal("0.08")));

        assertThat(result.subtotal()).isEqualByComparingTo("100.000000");
        assertThat(result.sgaCost()).isEqualByComparingTo("10.000000");
        assertThat(result.profitMargin()).isEqualByComparingTo("8.000000");
        assertThat(result.totalCost()).isEqualByComparingTo("118.000000");
    }

    @Test
    void calculateFullBuildUp_withNegativeSgaPct_throwsBusinessRuleViolation() {
        FullBuildUpInput input = new FullBuildUpInput(
                new BigDecimal("100.000000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("-0.01"), new BigDecimal("0.08"));

        assertThatThrownBy(() -> ShouldCostEngine.calculateFullBuildUp(input))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void calculateFullBuildUp_withNullMarginPct_throwsBusinessRuleViolation() {
        FullBuildUpInput input = new FullBuildUpInput(
                new BigDecimal("100.000000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("0.10"), null);

        assertThatThrownBy(() -> ShouldCostEngine.calculateFullBuildUp(input))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    // Overhead absorption: allocationBaseQuantity(40 labor hours) x absorptionRate(0.75/hr) = 30.
    @Test
    void calculateOverheadCost_matchesHandCalculatedControlValue() {
        BigDecimal overhead = ShouldCostEngine.calculateOverheadCost(
                new BigDecimal("40"), new BigDecimal("0.750000"));

        assertThat(overhead).isEqualByComparingTo("30.000000");
    }

    @Test
    void calculateOverheadCost_withNegativeAllocationBase_throwsBusinessRuleViolation() {
        assertThatThrownBy(() -> ShouldCostEngine.calculateOverheadCost(
                new BigDecimal("-1"), new BigDecimal("0.75")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    // TC-0102 (SC-02, Positive): 2.5kg x $12.40/kg = $31.00.
    @Test
    void calculateParametricCost_matchesHandCalculatedControlValue() {
        BigDecimal cost = ShouldCostEngine.calculateParametricCost(
                new BigDecimal("2.500000"), new BigDecimal("12.400000"));

        assertThat(cost).isEqualByComparingTo("31.000000");
    }

    @Test
    void calculateParametricCost_withZeroWeight_throwsBusinessRuleViolation() {
        assertThatThrownBy(() -> ShouldCostEngine.calculateParametricCost(
                BigDecimal.ZERO, new BigDecimal("12.4")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("SC-02");
    }

    @Test
    void calculateParametricCost_withNegativeCostPerKg_throwsBusinessRuleViolation() {
        assertThatThrownBy(() -> ShouldCostEngine.calculateParametricCost(
                new BigDecimal("2.5"), new BigDecimal("-1")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
