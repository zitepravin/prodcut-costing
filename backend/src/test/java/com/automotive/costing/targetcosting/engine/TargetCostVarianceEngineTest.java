package com.automotive.costing.targetcosting.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.targetcosting.engine.TargetCostVarianceEngine.VarianceInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TargetCostVarianceEngineTest {

    @Test
    void underTarget_negativeAbsolute() {
        var r = TargetCostVarianceEngine.calculate(new VarianceInput(
                new BigDecimal("100"), new BigDecimal("90"), new BigDecimal("0.05")));
        assertThat(r.absoluteVariance()).isEqualByComparingTo("-10.000000");
        assertThat(r.percentageVariance()).isEqualByComparingTo("-0.100000");
        assertThat(r.exceedsThreshold()).isTrue();
    }

    @Test
    void overTarget_withinThreshold() {
        var r = TargetCostVarianceEngine.calculate(new VarianceInput(
                new BigDecimal("100"), new BigDecimal("103"), new BigDecimal("0.05")));
        assertThat(r.absoluteVariance()).isEqualByComparingTo("3.000000");
        assertThat(r.percentageVariance()).isEqualByComparingTo("0.030000");
        assertThat(r.exceedsThreshold()).isFalse();
    }

    @Test
    void exactTarget_zeroVariance() {
        var r = TargetCostVarianceEngine.calculate(new VarianceInput(
                new BigDecimal("50"), new BigDecimal("50"), new BigDecimal("0.05")));
        assertThat(r.absoluteVariance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.percentageVariance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.exceedsThreshold()).isFalse();
    }

    @Test
    void nullThreshold_neverExceeds() {
        var r = TargetCostVarianceEngine.calculate(new VarianceInput(
                new BigDecimal("10"), new BigDecimal("20"), null));
        assertThat(r.percentageVariance()).isEqualByComparingTo("1.000000");
        assertThat(r.exceedsThreshold()).isFalse();
    }

    @Test
    void zeroTarget_throws() {
        assertThatThrownBy(() -> TargetCostVarianceEngine.calculate(new VarianceInput(
                BigDecimal.ZERO, new BigDecimal("1"), null)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("targetAmount");
    }

    @Test
    void invalidThreshold_throws() {
        assertThatThrownBy(() -> TargetCostVarianceEngine.calculate(new VarianceInput(
                new BigDecimal("10"), new BigDecimal("10"), BigDecimal.ONE)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("thresholdPct");
    }
}
