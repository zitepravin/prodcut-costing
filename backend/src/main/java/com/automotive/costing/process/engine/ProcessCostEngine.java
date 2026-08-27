package com.automotive.costing.process.engine;

import com.automotive.costing.common.costing.CostComponentResult;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.process.engine.ConsumableCostEngine.ConsumableLine;
import com.automotive.costing.process.engine.InspectionCostEngine.InspectionActivity;
import com.automotive.costing.process.engine.PowerCostEngine.PowerCostInput;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Process/labor cost calculation core (M04: PRC-02, PRC-04, PRC-06) extended for V277
 * component decomposition (SETUP, MACHINE, DIRECT_LABOR, POWER, CONSUMABLE, INSPECTION).
 *
 * Existing calculateOperation / calculateRouting remain backward compatible.
 */
public final class ProcessCostEngine {

    private static final int SCALE = 6;
    private static final BigDecimal SECONDS_PER_HOUR = new BigDecimal("3600");

    private ProcessCostEngine() {
    }

    public record OperationCostInput(
            String operationId,
            BigDecimal cycleTimeSeconds,
            BigDecimal machineRate,
            BigDecimal laborRate,
            BigDecimal operatorCount,
            BigDecimal setupTimeSeconds,
            BigDecimal batchSize,
            BigDecimal outputPartsPerCycle
    ) {
        public OperationCostInput(String operationId, BigDecimal cycleTimeSeconds, BigDecimal machineRate,
                                   BigDecimal laborRate, BigDecimal operatorCount, BigDecimal setupTimeSeconds,
                                   BigDecimal batchSize) {
            this(operationId, cycleTimeSeconds, machineRate, laborRate, operatorCount, setupTimeSeconds,
                    batchSize, null);
        }
    }

    /** Optional secondary components for V277 decomposition. */
    public record SecondaryCostInput(
            BigDecimal machineKw,
            BigDecimal electricityRate,
            String electricityRateCurrency,
            String electricityRateSource,
            LocalDate electricityRateEffectiveDate,
            List<ConsumableLine> consumables,
            List<InspectionActivity> inspections,
            BigDecimal externalProcessCost,
            LocalDate costingDate,
            String bomNodeId
    ) {
    }

    public record OperationCostResult(
            String operationId,
            BigDecimal runCost,
            BigDecimal setupCostPerPiece,
            BigDecimal totalOperationCost,
            String calculationBasis
    ) {
    }

    /** V277: operation result with explicit component breakdown. */
    public record OperationComponentResult(
            String operationId,
            BigDecimal setupCost,
            BigDecimal machineCost,
            BigDecimal directLaborCost,
            BigDecimal powerCost,
            BigDecimal consumableCost,
            BigDecimal inspectionCost,
            BigDecimal externalProcessCost,
            BigDecimal totalOperationCost,
            List<CostComponentResult> components,
            String calculationBasis
    ) {
    }

    public record RoutingCostResult(
            List<OperationCostResult> operationResults,
            BigDecimal totalProcessCost,
            String calculationBasis
    ) {
    }

    public static OperationCostResult calculateOperation(OperationCostInput input) {
        BigDecimal cycleTimeHours = requirePositive(input.cycleTimeSeconds(), "cycleTimeSeconds")
                .divide(SECONDS_PER_HOUR, 10, RoundingMode.HALF_UP);
        BigDecimal operatorCount = input.operatorCount() == null ? BigDecimal.ONE : input.operatorCount();

        BigDecimal machineComponent = cycleTimeHours.multiply(nz(input.machineRate()));
        BigDecimal laborComponent = cycleTimeHours.multiply(nz(input.laborRate())).multiply(operatorCount);
        BigDecimal runCost = machineComponent.add(laborComponent);

        BigDecimal partsPerCycle = input.outputPartsPerCycle() == null
                || input.outputPartsPerCycle().compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ONE : input.outputPartsPerCycle();
        if (partsPerCycle.compareTo(BigDecimal.ONE) > 0) {
            runCost = runCost.divide(partsPerCycle, 10, RoundingMode.HALF_UP);
        }

        BigDecimal setupPerPiece = calculateSetupCostPerPiece(input);
        BigDecimal total = runCost.add(setupPerPiece).setScale(SCALE, RoundingMode.HALF_UP);

        String basis = "op=" + input.operationId()
                + "; run=" + runCost.setScale(SCALE, RoundingMode.HALF_UP).toPlainString()
                + "; setupPerPiece=" + setupPerPiece.setScale(SCALE, RoundingMode.HALF_UP).toPlainString();

        return new OperationCostResult(
                input.operationId(),
                runCost.setScale(SCALE, RoundingMode.HALF_UP),
                setupPerPiece.setScale(SCALE, RoundingMode.HALF_UP),
                total,
                basis);
    }

    /**
     * V277: calculate operation with SETUP / MACHINE / DIRECT_LABOR / POWER / CONSUMABLE / INSPECTION
     * decomposition. Total equals sum of component amounts.
     */
    public static OperationComponentResult calculateOperationWithComponents(
            OperationCostInput input, SecondaryCostInput secondary) {

        BigDecimal cycleTimeHours = requirePositive(input.cycleTimeSeconds(), "cycleTimeSeconds")
                .divide(SECONDS_PER_HOUR, 10, RoundingMode.HALF_UP);
        BigDecimal operatorCount = input.operatorCount() == null ? BigDecimal.ONE : input.operatorCount();

        BigDecimal machineCost = cycleTimeHours.multiply(nz(input.machineRate()));
        BigDecimal laborCost = cycleTimeHours.multiply(nz(input.laborRate())).multiply(operatorCount);

        BigDecimal partsPerCycle = input.outputPartsPerCycle() == null
                || input.outputPartsPerCycle().compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ONE : input.outputPartsPerCycle();
        if (partsPerCycle.compareTo(BigDecimal.ONE) > 0) {
            machineCost = machineCost.divide(partsPerCycle, 10, RoundingMode.HALF_UP);
            laborCost = laborCost.divide(partsPerCycle, 10, RoundingMode.HALF_UP);
        }
        machineCost = machineCost.setScale(SCALE, RoundingMode.HALF_UP);
        laborCost = laborCost.setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal setupCost = calculateSetupCostPerPiece(input).setScale(SCALE, RoundingMode.HALF_UP);

        List<CostComponentResult> components = new ArrayList<>();
        if (setupCost.compareTo(BigDecimal.ZERO) > 0) {
            components.add(CostComponentResult.builder("SETUP")
                    .amount(setupCost).currency("USD").formulaCode("PRC-04")
                    .explanation("Setup amortized per piece")
                    .operationId(input.operationId()).build());
        }
        if (machineCost.compareTo(BigDecimal.ZERO) > 0) {
            components.add(CostComponentResult.builder("MACHINE")
                    .amount(machineCost).rate(input.machineRate()).quantity(cycleTimeHours)
                    .uom("h").formulaCode("PRC-02-MACHINE")
                    .explanation("Machine = cycleHours × machineRate / partsPerCycle")
                    .operationId(input.operationId()).build());
        }
        if (laborCost.compareTo(BigDecimal.ZERO) > 0) {
            components.add(CostComponentResult.builder("DIRECT_LABOR")
                    .amount(laborCost).rate(input.laborRate()).quantity(cycleTimeHours.multiply(operatorCount))
                    .uom("h").formulaCode("PRC-02-LABOR")
                    .explanation("Labor = cycleHours × laborRate × operators / partsPerCycle")
                    .operationId(input.operationId()).build());
        }

        BigDecimal powerCost = BigDecimal.ZERO;
        BigDecimal consumableCost = BigDecimal.ZERO;
        BigDecimal inspectionCost = BigDecimal.ZERO;
        BigDecimal externalCost = BigDecimal.ZERO;
        LocalDate costingDate = secondary != null ? secondary.costingDate() : null;
        String bomNodeId = secondary != null ? secondary.bomNodeId() : null;

        if (secondary != null) {
            if (secondary.machineKw() != null || secondary.electricityRate() != null) {
                // Runtime hours for power = cycle time hours (per piece after cavity split when applied)
                BigDecimal runtimeHours = cycleTimeHours;
                if (partsPerCycle.compareTo(BigDecimal.ONE) > 0) {
                    runtimeHours = cycleTimeHours.divide(partsPerCycle, 10, RoundingMode.HALF_UP);
                }
                CostComponentResult power = PowerCostEngine.calculate(new PowerCostInput(
                        input.operationId(),
                        secondary.machineKw(),
                        runtimeHours,
                        secondary.electricityRate(),
                        secondary.electricityRateCurrency(),
                        secondary.electricityRateSource(),
                        secondary.electricityRateEffectiveDate(),
                        costingDate,
                        bomNodeId));
                powerCost = power.amount();
                components.add(power);
            }
            if (secondary.consumables() != null && !secondary.consumables().isEmpty()) {
                List<CostComponentResult> cons = ConsumableCostEngine.calculate(
                        new ConsumableCostEngine.ConsumableCostInput(
                                input.operationId(), bomNodeId, secondary.consumables(), costingDate));
                for (CostComponentResult c : cons) {
                    consumableCost = consumableCost.add(c.amount());
                    components.add(c);
                }
            }
            if (secondary.inspections() != null && !secondary.inspections().isEmpty()) {
                List<CostComponentResult> insp = InspectionCostEngine.calculate(
                        new InspectionCostEngine.InspectionCostInput(
                                input.operationId(), bomNodeId, secondary.inspections(), costingDate));
                for (CostComponentResult c : insp) {
                    inspectionCost = inspectionCost.add(c.amount());
                    components.add(c);
                }
            }
            if (secondary.externalProcessCost() != null
                    && secondary.externalProcessCost().compareTo(BigDecimal.ZERO) > 0) {
                externalCost = secondary.externalProcessCost().setScale(SCALE, RoundingMode.HALF_UP);
                components.add(CostComponentResult.builder("EXTERNAL_PROCESS")
                        .amount(externalCost).formulaCode("PRC-EXT")
                        .explanation("External process cost")
                        .operationId(input.operationId()).bomNodeId(bomNodeId).build());
            }
        }

        powerCost = powerCost.setScale(SCALE, RoundingMode.HALF_UP);
        consumableCost = consumableCost.setScale(SCALE, RoundingMode.HALF_UP);
        inspectionCost = inspectionCost.setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal total = setupCost.add(machineCost).add(laborCost)
                .add(powerCost).add(consumableCost).add(inspectionCost).add(externalCost)
                .setScale(SCALE, RoundingMode.HALF_UP);

        String basis = "components setup=" + setupCost.toPlainString()
                + " machine=" + machineCost.toPlainString()
                + " labor=" + laborCost.toPlainString()
                + " power=" + powerCost.toPlainString()
                + " consumable=" + consumableCost.toPlainString()
                + " inspection=" + inspectionCost.toPlainString()
                + " external=" + externalCost.toPlainString();

        return new OperationComponentResult(
                input.operationId(), setupCost, machineCost, laborCost,
                powerCost, consumableCost, inspectionCost, externalCost,
                total, List.copyOf(components), basis);
    }

    public static RoutingCostResult calculateRouting(List<OperationCostInput> operations) {
        if (operations == null || operations.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "A process routing must have at least one operation before its cost can be calculated.",
                    "PRC-06");
        }

        List<OperationCostResult> results = operations.stream().map(ProcessCostEngine::calculateOperation).toList();
        BigDecimal total = results.stream()
                .map(OperationCostResult::totalOperationCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);

        String basis = "sum of " + results.size() + " operation(s): "
                + results.stream().map(r -> r.operationId() + "=" + r.totalOperationCost().toPlainString())
                        .reduce((a, b) -> a + ", " + b).orElse("");

        return new RoutingCostResult(results, total, basis);
    }

    private static BigDecimal calculateSetupCostPerPiece(OperationCostInput input) {
        if (input.setupTimeSeconds() == null || input.setupTimeSeconds().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (input.batchSize() == null || input.batchSize().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException(
                    "Operation " + input.operationId() + " has a setupTimeSeconds but no positive batchSize - "
                            + "setup cost per piece cannot be computed (SRS 4.4.5: batch size cannot be zero or blank).",
                    "PRC-04-batch-size-guard");
        }
        BigDecimal setupHours = input.setupTimeSeconds().divide(SECONDS_PER_HOUR, 10, RoundingMode.HALF_UP);
        BigDecimal totalSetupCost = setupHours.multiply(nz(input.machineRate()).add(nz(input.laborRate())));
        return totalSetupCost.divide(input.batchSize(), 10, RoundingMode.HALF_UP);
    }

    private static BigDecimal requirePositive(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException(field + " must be greater than zero.", "PRC-02");
        }
        return value;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
