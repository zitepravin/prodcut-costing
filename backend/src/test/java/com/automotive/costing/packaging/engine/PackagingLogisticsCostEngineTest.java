package com.automotive.costing.packaging.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.packaging.engine.PackagingLogisticsCostEngine.PackagingLogisticsCostInput;
import com.automotive.costing.packaging.entity.FreightMode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PackagingLogisticsCostEngineTest {

    @Test
    void packagingOnly_noFreightNoDuty() {
        var result = PackagingLogisticsCostEngine.calculate(new PackagingLogisticsCostInput(
                new BigDecimal("1.50"), new BigDecimal("0.25"), null, null, null,
                FreightMode.NONE, null, null, null));
        assertThat(result.packagingCostPerPiece()).isEqualByComparingTo("1.750000");
        assertThat(result.freightCostPerPiece()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.dutyCostPerPiece()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalPackagingFreightCost()).isEqualByComparingTo("1.750000");
    }

    @Test
    void returnableContainer_amortizedOverLifeCycles() {
        // container 200 / 100 cycles = 2.00 per piece
        var result = PackagingLogisticsCostEngine.calculate(new PackagingLogisticsCostInput(
                new BigDecimal("0.50"), BigDecimal.ZERO,
                new BigDecimal("200"), new BigDecimal("100"),
                null, FreightMode.NONE, null, null, null));
        assertThat(result.packagingCostPerPiece()).isEqualByComparingTo("2.500000");
    }

    @Test
    void returnableWithoutLifeCycles_throws() {
        assertThatThrownBy(() -> PackagingLogisticsCostEngine.calculate(new PackagingLogisticsCostInput(
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100"), null,
                null, FreightMode.NONE, null, null, null)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("containerLifeCycles");
    }

    @Test
    void outboundFreight_weightAndFixed() {
        // fixed 0.40 + 2.5kg * 0.80/kg = 0.40 + 2.00 = 2.40
        var result = PackagingLogisticsCostEngine.calculate(new PackagingLogisticsCostInput(
                BigDecimal.ZERO, BigDecimal.ZERO, null, null,
                new BigDecimal("2.5"), FreightMode.OUTBOUND,
                new BigDecimal("0.80"), new BigDecimal("0.40"), null));
        assertThat(result.freightCostPerPiece()).isEqualByComparingTo("2.400000");
        assertThat(result.totalPackagingFreightCost()).isEqualByComparingTo("2.400000");
    }

    @Test
    void bothDirections_doublesFreight() {
        var result = PackagingLogisticsCostEngine.calculate(new PackagingLogisticsCostInput(
                BigDecimal.ZERO, BigDecimal.ZERO, null, null,
                new BigDecimal("1.0"), FreightMode.BOTH,
                new BigDecimal("1.00"), BigDecimal.ZERO, null));
        assertThat(result.freightCostPerPiece()).isEqualByComparingTo("2.000000");
    }

    @Test
    void dutyAppliedToPackagingPlusFreight() {
        // pack 10 + freight 5 = 15; duty 10% = 1.50; total 16.50
        var result = PackagingLogisticsCostEngine.calculate(new PackagingLogisticsCostInput(
                new BigDecimal("10"), BigDecimal.ZERO, null, null,
                BigDecimal.ZERO, FreightMode.OUTBOUND,
                null, new BigDecimal("5"), new BigDecimal("0.10")));
        assertThat(result.dutyCostPerPiece()).isEqualByComparingTo("1.500000");
        assertThat(result.totalPackagingFreightCost()).isEqualByComparingTo("16.500000");
    }

    @Test
    void dutyAtOrAboveOne_throws() {
        assertThatThrownBy(() -> PackagingLogisticsCostEngine.calculate(new PackagingLogisticsCostInput(
                BigDecimal.ONE, BigDecimal.ZERO, null, null, null,
                FreightMode.NONE, null, null, BigDecimal.ONE)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("dutyPct");
    }

    @Test
    void weightRateWithoutWeight_throws() {
        assertThatThrownBy(() -> PackagingLogisticsCostEngine.calculate(new PackagingLogisticsCostInput(
                BigDecimal.ZERO, BigDecimal.ZERO, null, null, null,
                FreightMode.INBOUND, new BigDecimal("1.00"), null, null)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("netWeightKg");
    }

    @Test
    void negativePrimary_throws() {
        assertThatThrownBy(() -> PackagingLogisticsCostEngine.calculate(new PackagingLogisticsCostInput(
                new BigDecimal("-1"), BigDecimal.ZERO, null, null, null,
                FreightMode.NONE, null, null, null)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
