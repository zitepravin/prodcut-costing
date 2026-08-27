package com.automotive.costing.process.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.process.engine.ProcessCostEngine.OperationCostInput;
import com.automotive.costing.process.engine.ProcessCostEngine.OperationCostResult;
import com.automotive.costing.process.engine.ProcessCostEngine.RoutingCostResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ProcessCostEngine, mapped to QA_Test_Case_Tracker.xlsx Module M04
 * (PRC-02, PRC-04, PRC-05, PRC-06). Expected values hand-calculated and independently
 * cross-checked with Python's Decimal against BigDecimal HALF_UP semantics, per Test Plan
 * Section 12 risk #1 / TDD Section 6.4 - never asserted against the engine's own output.
 */
class ProcessCostEngineTest {

    // TC-0049/TC-0050 (PRC-01/02 basis): 45s cycle time, $60/hr machine, $25/hr labor,
    // 1 operator, no setup. cycleTimeHours = 45/3600 = 0.0125.
    // runCost = (0.0125 x 60) + (0.0125 x 25 x 1) = 0.75 + 0.3125 = 1.0625
    @Test
    void calculateOperation_withoutSetup_matchesHandCalculatedControlValue() {
        OperationCostResult result = ProcessCostEngine.calculateOperation(new OperationCostInput(
                "op-1", new BigDecimal("45"), new BigDecimal("60.000000"), new BigDecimal("25.000000"),
                BigDecimal.ONE, null, null));

        assertThat(result.runCost()).isEqualByComparingTo("1.062500");
        assertThat(result.setupCostPerPiece()).isEqualByComparingTo("0.000000");
        assertThat(result.totalOperationCost()).isEqualByComparingTo("1.062500");
    }

    // TC-0051/TC-0052 (PRC-04, Positive): same operation + 600s setup / batch of 50.
    // setupHours = 600/3600 = 0.1666666667; totalSetupCost = 0.1666666667 x (60+25) = 14.1666666695
    // setupCostPerPiece = 14.1666666695 / 50 = 0.283333 (scale6); total = 1.0625 + 0.283333 = 1.345833
    @Test
    void calculateOperation_withSetupAmortizedOverBatch_matchesHandCalculatedControlValue() {
        OperationCostResult result = ProcessCostEngine.calculateOperation(new OperationCostInput(
                "op-1", new BigDecimal("45"), new BigDecimal("60.000000"), new BigDecimal("25.000000"),
                BigDecimal.ONE, new BigDecimal("600"), new BigDecimal("50")));

        assertThat(result.setupCostPerPiece()).isEqualByComparingTo("0.283333");
        assertThat(result.totalOperationCost()).isEqualByComparingTo("1.345833");
    }

    // TC-0051/TC-0052 (PRC-04, Negative/Boundary): SRS 4.4.5 "batch size cannot be zero or blank".
    @Test
    void calculateOperation_withSetupTimeButZeroBatchSize_throwsBusinessRuleViolation() {
        OperationCostInput input = new OperationCostInput(
                "op-1", new BigDecimal("45"), new BigDecimal("60.000000"), new BigDecimal("25.000000"),
                BigDecimal.ONE, new BigDecimal("600"), BigDecimal.ZERO);

        assertThatThrownBy(() -> ProcessCostEngine.calculateOperation(input))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("PRC-04");
    }

    @Test
    void calculateOperation_withSetupTimeButNullBatchSize_throwsBusinessRuleViolation() {
        OperationCostInput input = new OperationCostInput(
                "op-1", new BigDecimal("45"), new BigDecimal("60.000000"), new BigDecimal("25.000000"),
                BigDecimal.ONE, new BigDecimal("600"), null);

        assertThatThrownBy(() -> ProcessCostEngine.calculateOperation(input))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    // PRC-04: no setupTimeSeconds at all is a legitimate "no setup for this op" input.
    @Test
    void calculateOperation_withoutSetupTime_appliesZeroSetupCostWithoutRequiringBatchSize() {
        OperationCostResult result = ProcessCostEngine.calculateOperation(new OperationCostInput(
                "op-1", new BigDecimal("45"), new BigDecimal("60.000000"), new BigDecimal("25.000000"),
                BigDecimal.ONE, null, null));

        assertThat(result.setupCostPerPiece()).isEqualByComparingTo("0.000000");
    }

    // TC-0053/TC-0054 (PRC-05, Positive): operator:machine ratio other than 1:1 - one
    // operator tending 3 molding machines is operatorCount = 0.333333 on each.
    // cycleTimeHours = 90/3600 = 0.025; run = (0.025 x 45) + (0.025 x 30 x 0.333333) = 1.125 + 0.25 = 1.375
    @Test
    void calculateOperation_withFractionalOperatorCount_matchesHandCalculatedControlValue() {
        OperationCostResult result = ProcessCostEngine.calculateOperation(new OperationCostInput(
                "op-1", new BigDecimal("90"), new BigDecimal("45.000000"), new BigDecimal("30.000000"),
                new BigDecimal("0.333333"), null, null));

        assertThat(result.totalOperationCost()).isEqualByComparingTo("1.375000");
    }

    // Machine-only operation (unattended machine time) - laborRate absent.
    @Test
    void calculateOperation_withNullLaborRate_treatsLaborComponentAsZero() {
        OperationCostResult result = ProcessCostEngine.calculateOperation(new OperationCostInput(
                "op-1", new BigDecimal("45"), new BigDecimal("60.000000"), null,
                BigDecimal.ONE, null, null));

        assertThat(result.totalOperationCost()).isEqualByComparingTo("0.750000");
    }

    // Labor-only operation (manual bench work, no machine) - machineRate absent.
    @Test
    void calculateOperation_withNullMachineRate_treatsMachineComponentAsZero() {
        OperationCostResult result = ProcessCostEngine.calculateOperation(new OperationCostInput(
                "op-1", new BigDecimal("45"), null, new BigDecimal("25.000000"),
                BigDecimal.ONE, null, null));

        assertThat(result.totalOperationCost()).isEqualByComparingTo("0.312500");
    }

    // TL-03 (M06 cross-reference, Positive): a 4-cavity mold with the same cycle time as
    // the op-1 control case above produces parts at 1/4 the per-piece process cost.
    // runCost = 1.0625 / 4 = 0.265625
    @Test
    void calculateOperation_withMultiCavityOutputPartsPerCycle_dividesRunCostByCavityCount() {
        OperationCostResult result = ProcessCostEngine.calculateOperation(new OperationCostInput(
                "op-1", new BigDecimal("45"), new BigDecimal("60.000000"), new BigDecimal("25.000000"),
                BigDecimal.ONE, null, null, new BigDecimal("4")));

        assertThat(result.totalOperationCost()).isEqualByComparingTo("0.265625");
    }

    // TL-03: absent outputPartsPerCycle defaults to 1 (single-cavity, no behavior change).
    @Test
    void calculateOperation_withoutOutputPartsPerCycle_defaultsToSingleCavity() {
        OperationCostResult withNull = ProcessCostEngine.calculateOperation(new OperationCostInput(
                "op-1", new BigDecimal("45"), new BigDecimal("60.000000"), new BigDecimal("25.000000"),
                BigDecimal.ONE, null, null, null));
        OperationCostResult withOne = ProcessCostEngine.calculateOperation(new OperationCostInput(
                "op-1", new BigDecimal("45"), new BigDecimal("60.000000"), new BigDecimal("25.000000"),
                BigDecimal.ONE, null, null, BigDecimal.ONE));

        assertThat(withNull.totalOperationCost()).isEqualByComparingTo(withOne.totalOperationCost());
        assertThat(withNull.totalOperationCost()).isEqualByComparingTo("1.062500");
    }

    @Test
    void calculateOperation_withZeroCycleTime_throwsBusinessRuleViolation() {
        OperationCostInput input = new OperationCostInput(
                "op-1", BigDecimal.ZERO, new BigDecimal("60.000000"), new BigDecimal("25.000000"),
                BigDecimal.ONE, null, null);

        assertThatThrownBy(() -> ProcessCostEngine.calculateOperation(input))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    // TC-0055/TC-0056-ish (PRC-06, Positive): total process cost = sum of every operation.
    @Test
    void calculateRouting_sumsAllOperationCosts() {
        OperationCostInput op1 = new OperationCostInput(
                "op-1", new BigDecimal("45"), new BigDecimal("60.000000"), new BigDecimal("25.000000"),
                BigDecimal.ONE, null, null); // 1.0625
        OperationCostInput op2 = new OperationCostInput(
                "op-2", new BigDecimal("90"), new BigDecimal("45.000000"), new BigDecimal("30.000000"),
                new BigDecimal("0.333333"), null, null); // 1.375

        RoutingCostResult result = ProcessCostEngine.calculateRouting(List.of(op1, op2));

        assertThat(result.operationResults()).hasSize(2);
        assertThat(result.totalProcessCost()).isEqualByComparingTo("2.437500");
    }

    // PRC-06 (Negative/Boundary): an empty routing cannot be costed.
    @Test
    void calculateRouting_withNoOperations_throwsBusinessRuleViolation() {
        assertThatThrownBy(() -> ProcessCostEngine.calculateRouting(List.of()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("PRC-06");
    }

    @Test
    void calculateRouting_withNullOperationList_throwsBusinessRuleViolation() {
        assertThatThrownBy(() -> ProcessCostEngine.calculateRouting(null))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
