package com.automotive.costing.tooling.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ToolingCostDtos {

    public record CalculateRequest(
            @NotBlank(message = "toolingRecordId is required") String toolingRecordId
    ) {
    }

    public record Response(
            String calculationId,
            String toolingRecordId,
            String bomNodeId,
            String programId,
            BigDecimal baseAmortizedCostPerPiece,
            Integer refurbishmentEventsProjected,
            BigDecimal refurbishmentCostPerPiece,
            BigDecimal totalAmortizedCostPerPiece,
            boolean includedInPiecePrice,
            String calculationBasis,
            Instant calculatedAt
    ) {
    }

    /** TL-06: side-by-side comparison across up to five tooling records (supplier proposals) for the same part. */
    public record CompareRequest(
            List<String> toolingRecordIds
    ) {
    }

    public record CompareLine(
            String toolingRecordId,
            String supplierId,
            BigDecimal totalAmortizedCostPerPiece,
            boolean includedInPiecePrice
    ) {
    }

    public record CompareResponse(
            List<CompareLine> lines,
            String lowestCostToolingRecordId
    ) {
    }
}
