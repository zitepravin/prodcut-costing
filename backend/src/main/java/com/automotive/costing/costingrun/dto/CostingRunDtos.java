package com.automotive.costing.costingrun.dto;

import com.automotive.costing.costingrun.entity.CostingRunItemStatus;
import com.automotive.costing.costingrun.entity.CostingRunStatus;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class CostingRunDtos {
    private CostingRunDtos() {}

    public record RunResponse(
            String runId, String runCode, String description, CostingRunStatus status,
            String programId, String plantCode, String costingVariantId, String costingSheetId,
            BigDecimal lotSize, LocalDate costingDate,
            int successCount, int failureCount,
            Instant startedAt, Instant completedAt
    ) {}

    public record ItemResponse(
            String itemId, String bomNodeId, String partNumber, CostingRunItemStatus status,
            BigDecimal unitCost, String errorMessage, String estimateBasis, Instant calculatedAt
    ) {}

    public record RunDetailResponse(RunResponse run, List<ItemResponse> items) {}

    public record CreateRunRequest(
            @NotBlank String runCode,
            String description,
            String programId,
            String plantCode,
            String costingVariantId,
            String costingSheetId,
            BigDecimal lotSize,
            LocalDate costingDate,
            /** Optional explicit BOM node list; if empty and programId set, all program nodes. */
            List<String> bomNodeIds
    ) {}
}
