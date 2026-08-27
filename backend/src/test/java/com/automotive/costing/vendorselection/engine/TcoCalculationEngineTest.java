package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.vendorselection.engine.TcoCalculationEngine.TcoInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TcoCalculationEngineTest {

    private final TcoCalculationEngine engine = new TcoCalculationEngine();

    @Test
    void sumsOnlyProvidedComponents_marksMissing() {
        var out = engine.calculate(new TcoInput(
                new BigDecimal("100.00"),
                new BigDecimal("5.00"),
                null, null, null, null, null, null, null, null, null,
                new BigDecimal("1000")));

        assertThat(out.tcoTotal()).isEqualByComparingTo("105.000000");
        assertThat(out.dataComplete()).isFalse();
        assertThat(out.missingComponents()).contains("WARRANTY_COST", "LOGISTICS");
        assertThat(out.components().stream()
                .filter(c -> "FREIGHT".equals(c.code()))
                .findFirst().orElseThrow().status()).isEqualTo("PROVIDED");
        assertThat(out.components().stream()
                .filter(c -> "WARRANTY_COST".equals(c.code()))
                .findFirst().orElseThrow().status()).isEqualTo(TcoCalculationEngine.DATA_NOT_AVAILABLE);
    }

    @Test
    void nullIsNeverTreatedAsZero() {
        var out = engine.calculate(new TcoInput(
                new BigDecimal("50"), null, null, null, null, null, null, null, null, null, null, null));
        assertThat(out.tcoTotal()).isEqualByComparingTo("50.000000");
        assertThat(out.missingComponents()).hasSize(10);
    }

    @Test
    void completeWhenAllComponentsProvided() {
        var out = engine.calculate(new TcoInput(
                bd("10"), bd("1"), bd("1"), bd("1"), bd("1"),
                bd("1"), bd("1"), bd("1"), bd("1"), bd("1"), bd("1"),
                bd("100")));
        assertThat(out.dataComplete()).isTrue();
        assertThat(out.missingComponents()).isEmpty();
        assertThat(out.tcoTotal()).isEqualByComparingTo("20.000000");
        assertThat(out.unitTco()).isEqualByComparingTo("0.200000");
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }
}
