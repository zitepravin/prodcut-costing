package com.automotive.costing.erp.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.importing.ExcelSheetReader;
import com.automotive.costing.erp.dto.ErpDtos.BomRow;
import com.automotive.costing.erp.dto.ErpDtos.CsvIngestResult;
import com.automotive.costing.erp.dto.ErpDtos.CsvRowError;
import com.automotive.costing.erp.dto.ErpDtos.IngestRequest;
import com.automotive.costing.erp.dto.ErpDtos.MaterialPriceRow;
import com.automotive.costing.erp.dto.ErpDtos.PartRow;
import com.automotive.costing.erp.entity.ErpBatchType;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Excel (.xlsx) bulk ingest into ERP staging. Same templates and partial-success
 * contract as {@link ErpStagingCsvImportService}.
 */
@Service
@RequiredArgsConstructor
public class ErpStagingExcelImportService {

    private final ErpStagingService erpStagingService;

    public CsvIngestResult importMaterialPrices(MultipartFile file, String sourceSystem) throws IOException {
        List<CsvRowError> errors = new ArrayList<>();
        List<MaterialPriceRow> rows = new ArrayList<>();
        int total = 0;

        try (Workbook workbook = ExcelSheetReader.openWorkbook(file)) {
            Sheet sheet = ExcelSheetReader.requireFirstSheet(workbook);
            String header = ExcelSheetReader.headerLine(sheet, 8);
            if (!ExcelSheetReader.headersMatch(header, ErpStagingCsvImportService.MATERIAL_PRICE_HEADER)) {
                errors.add(new CsvRowError(1, "Header must be: " + ErpStagingCsvImportService.MATERIAL_PRICE_HEADER
                        + " (found: " + header + ")"));
                return new CsvIngestResult(null, 0, 0, 1, errors);
            }
            Iterator<Row> it = ExcelSheetReader.dataRows(sheet);
            while (it.hasNext()) {
                Row row = it.next();
                int rowNum = row.getRowNum() + 1;
                if (ExcelSheetReader.isBlankRow(row, 8)) {
                    continue;
                }
                total++;
                try {
                    rows.add(new MaterialPriceRow(
                            ExcelSheetReader.requireString(row, 0, "materialCode"),
                            ExcelSheetReader.optionalString(row, 5),
                            ExcelSheetReader.requireDecimal(row, 1, "unitPrice"),
                            ExcelSheetReader.requireString(row, 2, "currencyCode").toUpperCase(),
                            ExcelSheetReader.optionalString(row, 6),
                            ExcelSheetReader.requireDate(row, 3, "effectiveFrom"),
                            ExcelSheetReader.optionalDate(row, 4, "effectiveTo"),
                            ExcelSheetReader.optionalString(row, 7),
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
                sourceOrDefault(sourceSystem),
                ErpBatchType.MATERIAL_PRICE,
                file.getOriginalFilename(),
                "Excel material price import",
                null, null, rows
        ));
        return new CsvIngestResult(batch, total, rows.size(), errors.size(), errors);
    }

    public CsvIngestResult importParts(MultipartFile file, String sourceSystem) throws IOException {
        List<CsvRowError> errors = new ArrayList<>();
        List<PartRow> rows = new ArrayList<>();
        int total = 0;

        try (Workbook workbook = ExcelSheetReader.openWorkbook(file)) {
            Sheet sheet = ExcelSheetReader.requireFirstSheet(workbook);
            String header = ExcelSheetReader.headerLine(sheet, 10);
            if (!ExcelSheetReader.headersMatch(header, ErpStagingCsvImportService.PART_HEADER)) {
                errors.add(new CsvRowError(1, "Header must be: " + ErpStagingCsvImportService.PART_HEADER
                        + " (found: " + header + ")"));
                return new CsvIngestResult(null, 0, 0, 1, errors);
            }
            Iterator<Row> it = ExcelSheetReader.dataRows(sheet);
            while (it.hasNext()) {
                Row row = it.next();
                int rowNum = row.getRowNum() + 1;
                if (ExcelSheetReader.isBlankRow(row, 10)) {
                    continue;
                }
                total++;
                try {
                    rows.add(new PartRow(
                            ExcelSheetReader.requireString(row, 0, "externalPartId"),
                            ExcelSheetReader.requireString(row, 1, "partNumber"),
                            ExcelSheetReader.optionalString(row, 2),
                            ExcelSheetReader.optionalString(row, 3),
                            ExcelSheetReader.optionalDecimal(row, 4, "netWeightKg"),
                            ExcelSheetReader.optionalString(row, 5),
                            ExcelSheetReader.optionalString(row, 6),
                            ExcelSheetReader.optionalString(row, 7),
                            ExcelSheetReader.optionalString(row, 8),
                            ExcelSheetReader.optionalString(row, 9),
                            null
                    ));
                } catch (Exception e) {
                    errors.add(new CsvRowError(rowNum, e.getMessage()));
                }
            }
        }

        if (rows.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "No valid part rows to ingest (" + errors.size() + " parse errors)", "ERP-07");
        }

        var batch = erpStagingService.ingest(new IngestRequest(
                sourceOrDefault(sourceSystem),
                ErpBatchType.PART,
                file.getOriginalFilename(),
                "Excel part import",
                rows, null, null
        ));
        return new CsvIngestResult(batch, total, rows.size(), errors.size(), errors);
    }

    public CsvIngestResult importBom(MultipartFile file, String sourceSystem) throws IOException {
        List<CsvRowError> errors = new ArrayList<>();
        List<BomRow> rows = new ArrayList<>();
        int total = 0;

        try (Workbook workbook = ExcelSheetReader.openWorkbook(file)) {
            Sheet sheet = ExcelSheetReader.requireFirstSheet(workbook);
            String header = ExcelSheetReader.headerLine(sheet, 9);
            if (!ExcelSheetReader.headersMatch(header, ErpStagingCsvImportService.BOM_HEADER)) {
                errors.add(new CsvRowError(1, "Header must be: " + ErpStagingCsvImportService.BOM_HEADER
                        + " (found: " + header + ")"));
                return new CsvIngestResult(null, 0, 0, 1, errors);
            }
            Iterator<Row> it = ExcelSheetReader.dataRows(sheet);
            while (it.hasNext()) {
                Row row = it.next();
                int rowNum = row.getRowNum() + 1;
                if (ExcelSheetReader.isBlankRow(row, 9)) {
                    continue;
                }
                total++;
                try {
                    rows.add(new BomRow(
                            null,
                            ExcelSheetReader.requireString(row, 0, "parentPartNumber"),
                            ExcelSheetReader.optionalString(row, 3),
                            ExcelSheetReader.requireString(row, 1, "childPartNumber"),
                            ExcelSheetReader.optionalString(row, 4),
                            ExcelSheetReader.requireDecimal(row, 2, "quantityPer"),
                            ExcelSheetReader.optionalString(row, 5),
                            ExcelSheetReader.optionalInt(row, 6, "lineNumber"),
                            ExcelSheetReader.optionalString(row, 7),
                            ExcelSheetReader.optionalString(row, 8),
                            null
                    ));
                } catch (Exception e) {
                    errors.add(new CsvRowError(rowNum, e.getMessage()));
                }
            }
        }

        if (rows.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "No valid BOM rows to ingest (" + errors.size() + " parse errors)", "ERP-07");
        }

        var batch = erpStagingService.ingest(new IngestRequest(
                sourceOrDefault(sourceSystem),
                ErpBatchType.BOM,
                file.getOriginalFilename(),
                "Excel BOM import",
                null, rows, null
        ));
        return new CsvIngestResult(batch, total, rows.size(), errors.size(), errors);
    }

    private static String sourceOrDefault(String sourceSystem) {
        return sourceSystem != null && !sourceSystem.isBlank() ? sourceSystem : "EXCEL";
    }
}
