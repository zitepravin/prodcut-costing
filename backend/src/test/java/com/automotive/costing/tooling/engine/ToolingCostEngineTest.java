package com.automotive.costing.tooling.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.tooling.engine.ToolingCostEngine.ToolingCostInput;
import com.automotive.costing.tooling.engine.ToolingCostEngine.ToolingCostResult;
import com.automotive.costing.tooling.entity.ToolingOwnership;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ToolingCostEngine, mapped to QA_Test_Case_Tracker.xlsx Module M06
 * (TL-02, TL-04, TL-05). Expected values hand-calculated and independently cross-checked
 * with Python's Decimal against BigDecimal HALF_UP/CEILING semantics.
 */
class ToolingCostEngineTest {

    // TC-0079 (TL-02, Positive): toolCost=500,000 / lifetimeVolume=100,000 = 5.00/piece, no tool-life limit.
    @Test
    void calculate_withoutToolLifeLimit_appliesBaseAmortizationOnly() {
        ToolingCostResult result = ToolingCostEngine.calculate(new ToolingCostInput(
                new BigDecimal("500000.000000"), new BigDecimal("100000"), null, null, ToolingOwnership.SUPPLIER));

        assertThat(result.baseAmortizedCostPerPiece()).isEqualByComparingTo("5.000000");
        assertThat(result.refurbishmentEventsProjected()).isZero();
        assertThat(result.refurbishmentCostPerPiece()).isEqualByComparingTo("0.000000");
        assertThat(result.totalAmortizedCostPerPiece()).isEqualByComparingTo("5.000000");
    }

    // TC-0080 (TL-02, Negative/Boundary): SRS 4.6.5 hard rule - no lifetime volume, no calculation.
    @Test
    void calculate_withoutProgramLifetimeVolume_throwsBusinessRuleViolation() {
        ToolingCostInput input = new ToolingCostInput(
                new BigDecimal("500000.000000"), null, null, null, ToolingOwnership.SUPPLIER);

        assertThatThrownBy(() -> ToolingCostEngine.calculate(input))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("lifetime volume");
    }

    @Test
    void calculate_withZeroProgramLifetimeVolume_throwsBusinessRuleViolation() {
        ToolingCostInput input = new ToolingCostInput(
                new BigDecimal("500000.000000"), BigDecimal.ZERO, null, null, ToolingOwnership.SUPPLIER);

        assertThatThrownBy(() -> ToolingCostEngine.calculate(input))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void calculate_withZeroToolCost_throwsBusinessRuleViolation() {
        ToolingCostInput input = new ToolingCostInput(
                BigDecimal.ZERO, new BigDecimal("100000"), null, null, ToolingOwnership.SUPPLIER);

        assertThatThrownBy(() -> ToolingCostEngine.calculate(input))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("TL-02");
    }

    // TC-0083 (TL-04, Positive): lifetimeVolume(250,000) exceeds toolLifeHits(100,000) by
    // 2.5x -> ceil(2.5) = 3 life-cycles -> 2 refurbishment events.
    // base = 500,000/250,000 = 2.0; refurb = (2 x 50,000)/250,000 = 0.4; total = 2.4
    @Test
    void calculate_withProgramVolumeExceedingToolLife_triggersRefurbishmentAmortization() {
        ToolingCostResult result = ToolingCostEngine.calculate(new ToolingCostInput(
                new BigDecimal("500000.000000"), new BigDecimal("250000"), new BigDecimal("100000.00"),
                new BigDecimal("50000.000000"), ToolingOwnership.SUPPLIER));

        assertThat(result.refurbishmentEventsProjected()).isEqualTo(2);
        assertThat(result.refurbishmentCostPerPiece()).isEqualByComparingTo("0.400000");
        assertThat(result.totalAmortizedCostPerPiece()).isEqualByComparingTo("2.400000");
    }

    // TC-0084 (TL-04, Negative/Boundary): lifetimeVolume within a single tool-life -> no refurbishment.
    @Test
    void calculate_withProgramVolumeWithinToolLife_appliesNoRefurbishment() {
        ToolingCostResult result = ToolingCostEngine.calculate(new ToolingCostInput(
                new BigDecimal("500000.000000"), new BigDecimal("80000"), new BigDecimal("100000.00"),
                new BigDecimal("50000.000000"), ToolingOwnership.SUPPLIER));

        assertThat(result.refurbishmentEventsProjected()).isZero();
        assertThat(result.refurbishmentCostPerPiece()).isEqualByComparingTo("0.000000");
    }

    // TL-04: refurbishment triggered but no cost-per-event supplied -> treated as zero, not an error.
    @Test
    void calculate_withRefurbishmentTriggeredButNoCostPerEvent_treatsRefurbishmentCostAsZero() {
        ToolingCostResult result = ToolingCostEngine.calculate(new ToolingCostInput(
                new BigDecimal("500000.000000"), new BigDecimal("250000"), new BigDecimal("100000.00"),
                null, ToolingOwnership.SUPPLIER));

        assertThat(result.refurbishmentEventsProjected()).isEqualTo(2);
        assertThat(result.refurbishmentCostPerPiece()).isEqualByComparingTo("0.000000");
    }

    // TC-0085 (TL-05, Positive): Supplier-owned tooling IS included in piece price.
    @Test
    void calculate_withSupplierOwnership_isIncludedInPiecePrice() {
        ToolingCostResult result = ToolingCostEngine.calculate(new ToolingCostInput(
                new BigDecimal("500000.000000"), new BigDecimal("100000"), null, null, ToolingOwnership.SUPPLIER));

        assertThat(result.includedInPiecePrice()).isTrue();
    }

    // TC-0086 (TL-05, Negative/Boundary counterpart): Customer-owned tooling is EXCLUDED from
    // piece price (tracked separately) even though its amortized cost is still fully computed.
    @Test
    void calculate_withCustomerOwnership_isExcludedFromPiecePriceButStillComputed() {
        ToolingCostResult result = ToolingCostEngine.calculate(new ToolingCostInput(
                new BigDecimal("500000.000000"), new BigDecimal("100000"), null, null, ToolingOwnership.CUSTOMER));

        assertThat(result.includedInPiecePrice()).isFalse();
        assertThat(result.totalAmortizedCostPerPiece()).isEqualByComparingTo("5.000000");
    }
}
