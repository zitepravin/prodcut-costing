package com.automotive.costing.process.dto;

import com.automotive.costing.process.entity.RoutingStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class ProcessRoutingDtos {

    public record OperationRequest(
            @NotNull(message = "sequenceNumber is required") Integer sequenceNumber,
            String processName,
            String machineCode,
            String laborCode,
            @NotNull(message = "cycleTimeSeconds is required")
            @DecimalMin(value = "0.0", inclusive = false, message = "cycleTimeSeconds must be > 0")
            BigDecimal cycleTimeSeconds,
            @DecimalMin(value = "0.0", inclusive = false, message = "operatorCount must be > 0 if supplied")
            BigDecimal operatorCount,
            BigDecimal setupTimeSeconds,
            BigDecimal batchSize,
            /** TL-03: multi-cavity/multi-impression tooling output per cycle. Omit/null = 1 (single-cavity). */
            BigDecimal outputPartsPerCycle,
            /** V277 power / inspection / consumables */
            BigDecimal machineKw,
            BigDecimal electricityRate,
            String electricityRateCurrency,
            BigDecimal inspectionTimeHours,
            BigDecimal inspectorRate,
            BigDecimal inspectionFixedCharge,
            /** JSON array of consumable lines; validated by ProcessCostService on calculate. */
            String consumablesJson
    ) {
        public OperationRequest(Integer sequenceNumber, String processName, String machineCode, String laborCode,
                                 BigDecimal cycleTimeSeconds, BigDecimal operatorCount, BigDecimal setupTimeSeconds,
                                 BigDecimal batchSize) {
            this(sequenceNumber, processName, machineCode, laborCode, cycleTimeSeconds, operatorCount,
                    setupTimeSeconds, batchSize, null, null, null, null, null, null, null, null);
        }

        public OperationRequest(Integer sequenceNumber, String processName, String machineCode, String laborCode,
                                 BigDecimal cycleTimeSeconds, BigDecimal operatorCount, BigDecimal setupTimeSeconds,
                                 BigDecimal batchSize, BigDecimal outputPartsPerCycle) {
            this(sequenceNumber, processName, machineCode, laborCode, cycleTimeSeconds, operatorCount,
                    setupTimeSeconds, batchSize, outputPartsPerCycle, null, null, null, null, null, null, null);
        }
    }

    public record CreateRoutingRequest(
            @NotBlank(message = "bomNodeId is required") String bomNodeId,
            @NotEmpty(message = "a routing must have at least one operation") @Valid
            List<OperationRequest> operations
    ) {
    }

    public record OperationResponse(
            String operationId,
            Integer sequenceNumber,
            String processName,
            String machineCode,
            String laborCode,
            BigDecimal cycleTimeSeconds,
            BigDecimal operatorCount,
            BigDecimal setupTimeSeconds,
            BigDecimal batchSize,
            BigDecimal outputPartsPerCycle,
            BigDecimal machineKw,
            BigDecimal electricityRate,
            String electricityRateCurrency,
            BigDecimal inspectionTimeHours,
            BigDecimal inspectorRate,
            BigDecimal inspectionFixedCharge,
            String consumablesJson
    ) {
    }

    public record RoutingResponse(
            String routingId,
            String bomNodeId,
            RoutingStatus status,
            List<OperationResponse> operations
    ) {
    }

    public record CycleTimeTemplateResponse(
            String processName,
            String defaultCycleTimeFormula,
            java.math.BigDecimal defaultScrapPct
    ) {
    }
}
