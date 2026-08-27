package com.automotive.costing.erp.dto;

import com.automotive.costing.erp.entity.ErpBatchStatus;
import com.automotive.costing.erp.entity.ErpBatchType;
import com.automotive.costing.erp.entity.ErpStagingRowStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ErpDtos {

    private ErpDtos() {}

    public record PartRow(
            @NotBlank @Size(max = 100) String externalPartId,
            @NotBlank @Size(max = 50) String partNumber,
            @Size(max = 20) String revision,
            @Size(max = 500) String description,
            BigDecimal netWeightKg,
            @Size(max = 20) String uom,
            @Size(max = 50) String materialCode,
            @Size(max = 10) String makeBuyCode,
            @Size(max = 50) String plantCode,
            @Size(max = 50) String programCode,
            String rawPayload
    ) {}

    public record BomRow(
            @Size(max = 100) String externalBomId,
            @NotBlank @Size(max = 50) String parentPartNumber,
            @Size(max = 20) String parentRevision,
            @NotBlank @Size(max = 50) String childPartNumber,
            @Size(max = 20) String childRevision,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal quantityPer,
            @Size(max = 20) String uom,
            Integer lineNumber,
            @Size(max = 50) String plantCode,
            @Size(max = 50) String programCode,
            String rawPayload
    ) {}

    public record MaterialPriceRow(
            @NotBlank @Size(max = 50) String materialCode,
            @Size(max = 50) String plantCode,
            @NotNull @DecimalMin("0.0") BigDecimal unitPrice,
            @NotBlank @Size(min = 3, max = 3) String currencyCode,
            @Size(max = 20) String uom,
            @NotNull LocalDate effectiveFrom,
            LocalDate effectiveTo,
            @Size(max = 50) String supplierCode,
            String rawPayload
    ) {}

    public record IngestRequest(
            @NotBlank @Size(max = 50) String sourceSystem,
            @NotNull ErpBatchType batchType,
            @Size(max = 255) String sourceFileName,
            @Size(max = 1000) String notes,
            @Valid List<PartRow> parts,
            @Valid List<BomRow> bomLines,
            @Valid List<MaterialPriceRow> materialPrices
    ) {}

    public record BatchResponse(
            String batchId,
            String sourceSystem,
            ErpBatchType batchType,
            ErpBatchStatus status,
            int rowCount,
            int errorCount,
            String sourceFileName,
            Instant receivedAt,
            Instant processedAt
    ) {}

    public record PartStagingResponse(
            String stagingId,
            String batchId,
            String partNumber,
            String revision,
            BigDecimal netWeightKg,
            ErpStagingRowStatus status,
            String errorMessage
    ) {}

    public record BatchDetailResponse(
            BatchResponse batch,
            List<PartStagingResponse> parts,
            int bomLineCount,
            int materialPriceCount
    ) {}

    public record ValidateResult(
            String batchId,
            ErpBatchStatus status,
            int validRows,
            int invalidRows,
            List<String> messages
    ) {}

    /**
     * Result of promote-to-live for a READY batch.
     * Material prices → MaterialMaster versions; parts acknowledged;
     * BOM lines → live BomNode tree when program_code resolves.
     */
    public record PromoteResult(
            String batchId,
            ErpBatchStatus status,
            int materialPricesPromoted,
            int materialPricesSkipped,
            int partsAcknowledged,
            int bomLinesPromoted,
            int bomLinesSkipped,
            List<String> messages
    ) {}

    public record CsvRowError(int rowNumber, String reason) {}

    /**
     * Result of CSV bulk ingest: creates one staging batch from successfully parsed rows.
     * Parse errors do not block valid rows (partial success).
     */
    public record CsvIngestResult(
            BatchResponse batch,
            int totalRows,
            int acceptedRows,
            int rejectedRows,
            List<CsvRowError> errors
    ) {}
}
