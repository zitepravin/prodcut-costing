package com.automotive.costing.erp.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.erp.dto.ErpDtos.BomRow;
import com.automotive.costing.erp.dto.ErpDtos.CsvIngestResult;
import com.automotive.costing.erp.dto.ErpDtos.CsvRowError;
import com.automotive.costing.erp.dto.ErpDtos.IngestRequest;
import com.automotive.costing.erp.dto.ErpDtos.MaterialPriceRow;
import com.automotive.costing.erp.dto.ErpDtos.PartRow;
import com.automotive.costing.erp.entity.ErpBatchType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV bulk ingest into ERP staging. Parses rows independently (partial success),
 * then creates a single staging batch via {@link ErpStagingService#ingest}.
 *
 * Templates (header row required, exact column order):
 * <ul>
 *   <li>MATERIAL_PRICE: materialCode,unitPrice,currencyCode,effectiveFrom,effectiveTo,plantCode,uom,supplierCode</li>
 *   <li>PART: externalPartId,partNumber,revision,description,netWeightKg,uom,materialCode,makeBuyCode,plantCode,programCode</li>
 *   <li>BOM: parentPartNumber,childPartNumber,quantityPer,parentRevision,childRevision,uom,lineNumber,plantCode,programCode</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ErpStagingCsvImportService {

    public static final String MATERIAL_PRICE_HEADER =
            "materialCode,unitPrice,currencyCode,effectiveFrom,effectiveTo,plantCode,uom,supplierCode";
    public static final String PART_HEADER =
            "externalPartId,partNumber,revision,description,netWeightKg,uom,materialCode,makeBuyCode,plantCode,programCode";
    public static final String BOM_HEADER =
            "parentPartNumber,childPartNumber,quantityPer,parentRevision,childRevision,uom,lineNumber,plantCode,programCode";

    private final ErpStagingService erpStagingService;

    public CsvIngestResult importMaterialPrices(MultipartFile file, String sourceSystem) throws IOException {
        List<CsvRowError> errors = new ArrayList<>();
        List<MaterialPriceRow> rows = new ArrayList<>();
        int total = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null || !header.trim().equalsIgnoreCase(MATERIAL_PRICE_HEADER)) {
                errors.add(new CsvRowError(1, "Header must be: " + MATERIAL_PRICE_HEADER));
                return new CsvIngestResult(null, 0, 0, 1, errors);
            }
            String line;
            int rowNum = 1;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (line.isBlank()) {
                    continue;
                }
                total++;
                try {
                    String[] c = splitCsv(line, 8);
                    rows.add(new MaterialPriceRow(
                            require(c[0], "materialCode"),
                            blankToNull(c[5]),
                            new BigDecimal(c[1].trim()),
                            require(c[2], "currencyCode").toUpperCase(),
                            blankToNull(c[6]),
                            LocalDate.parse(c[3].trim()),
                            blankToNull(c[4]) != null ? LocalDate.parse(c[4].trim()) : null,
                            blankToNull(c[7]),
                            null
                    ));
                } catch (Exception e) {
                    errors.add(new CsvRowError(rowNum, e.getMessage()));
                }
            }
        }

        if (rows.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "No valid material price rows to ingest (" + errors.size() + " parse errors)",
                    "ERP-07");
        }

        var batch = erpStagingService.ingest(new IngestRequest(
                sourceSystem != null && !sourceSystem.isBlank() ? sourceSystem : "CSV",
                ErpBatchType.MATERIAL_PRICE,
                file.getOriginalFilename(),
                "CSV material price import",
                null,
                null,
                rows
        ));
        return new CsvIngestResult(batch, total, rows.size(), errors.size(), errors);
    }

    public CsvIngestResult importParts(MultipartFile file, String sourceSystem) throws IOException {
        List<CsvRowError> errors = new ArrayList<>();
        List<PartRow> rows = new ArrayList<>();
        int total = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null || !header.trim().equalsIgnoreCase(PART_HEADER)) {
                errors.add(new CsvRowError(1, "Header must be: " + PART_HEADER));
                return new CsvIngestResult(null, 0, 0, 1, errors);
            }
            String line;
            int rowNum = 1;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (line.isBlank()) {
                    continue;
                }
                total++;
                try {
                    String[] c = splitCsv(line, 10);
                    BigDecimal netWt = blankToNull(c[4]) != null ? new BigDecimal(c[4].trim()) : null;
                    rows.add(new PartRow(
                            require(c[0], "externalPartId"),
                            require(c[1], "partNumber"),
                            blankToNull(c[2]),
                            blankToNull(c[3]),
                            netWt,
                            blankToNull(c[5]),
                            blankToNull(c[6]),
                            blankToNull(c[7]),
                            blankToNull(c[8]),
                            blankToNull(c[9]),
                            null
                    ));
                } catch (Exception e) {
                    errors.add(new CsvRowError(rowNum, e.getMessage()));
                }
            }
        }

        if (rows.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "No valid part rows to ingest (" + errors.size() + " parse errors)",
                    "ERP-07");
        }

        var batch = erpStagingService.ingest(new IngestRequest(
                sourceSystem != null && !sourceSystem.isBlank() ? sourceSystem : "CSV",
                ErpBatchType.PART,
                file.getOriginalFilename(),
                "CSV part import",
                rows,
                null,
                null
        ));
        return new CsvIngestResult(batch, total, rows.size(), errors.size(), errors);
    }

    public CsvIngestResult importBom(MultipartFile file, String sourceSystem) throws IOException {
        List<CsvRowError> errors = new ArrayList<>();
        List<BomRow> rows = new ArrayList<>();
        int total = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null || !header.trim().equalsIgnoreCase(BOM_HEADER)) {
                errors.add(new CsvRowError(1, "Header must be: " + BOM_HEADER));
                return new CsvIngestResult(null, 0, 0, 1, errors);
            }
            String line;
            int rowNum = 1;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (line.isBlank()) {
                    continue;
                }
                total++;
                try {
                    String[] c = splitCsv(line, 9);
                    Integer lineNo = blankToNull(c[6]) != null ? Integer.parseInt(c[6].trim()) : null;
                    rows.add(new BomRow(
                            null,
                            require(c[0], "parentPartNumber"),
                            blankToNull(c[3]),
                            require(c[1], "childPartNumber"),
                            blankToNull(c[4]),
                            new BigDecimal(c[2].trim()),
                            blankToNull(c[5]),
                            lineNo,
                            blankToNull(c[7]),
                            blankToNull(c[8]),
                            null
                    ));
                } catch (Exception e) {
                    errors.add(new CsvRowError(rowNum, e.getMessage()));
                }
            }
        }

        if (rows.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "No valid BOM rows to ingest (" + errors.size() + " parse errors)",
                    "ERP-07");
        }

        var batch = erpStagingService.ingest(new IngestRequest(
                sourceSystem != null && !sourceSystem.isBlank() ? sourceSystem : "CSV",
                ErpBatchType.BOM,
                file.getOriginalFilename(),
                "CSV BOM import",
                null,
                rows,
                null
        ));
        return new CsvIngestResult(batch, total, rows.size(), errors.size(), errors);
    }

    private static String[] splitCsv(String line, int expectedMin) {
        String[] parts = line.split(",", -1);
        if (parts.length < expectedMin) {
            throw new IllegalArgumentException(
                    "Expected at least " + expectedMin + " columns, found " + parts.length);
        }
        return parts;
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
