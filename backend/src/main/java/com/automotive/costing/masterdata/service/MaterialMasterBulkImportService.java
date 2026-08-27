package com.automotive.costing.masterdata.service;

import com.automotive.costing.common.exception.DuplicateResourceException;
import com.automotive.costing.common.importing.ExcelSheetReader;
import com.automotive.costing.masterdata.dto.BulkImportDtos.ImportResult;
import com.automotive.costing.masterdata.dto.BulkImportDtos.RowError;
import com.automotive.costing.masterdata.dto.MaterialMasterDtos.CreateRequest;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * MDM-04: bulk import of material master records.
 * Supports CSV and Excel (.xlsx) with the same column template and partial-success contract.
 *
 * Template header (row 1, exact column order):
 * code,description,rate,uom,plantId,effectiveFrom,grade,specificationStandard,density,scrapRecoveryRate,materialForm
 */
@Service
@RequiredArgsConstructor
public class MaterialMasterBulkImportService {

    public static final String EXPECTED_HEADER =
            "code,description,rate,uom,plantId,effectiveFrom,grade,specificationStandard,density,scrapRecoveryRate,materialForm";
    private static final int COLUMN_COUNT = 11;

    private final MaterialMasterService materialMasterService;

    /**
     * Dispatches by filename extension: .xlsx/.xls → Excel path; otherwise CSV.
     */
    public ImportResult importFile(MultipartFile file) throws IOException {
        if (ExcelSheetReader.isExcelFilename(file.getOriginalFilename())) {
            return importFromExcel(file);
        }
        return importFromCsv(file);
    }

    public ImportResult importFromCsv(MultipartFile file) throws IOException {
        List<RowError> errors = new ArrayList<>();
        int total = 0;
        int succeeded = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String header = reader.readLine();
            if (header == null || !header.trim().equalsIgnoreCase(EXPECTED_HEADER)) {
                errors.add(new RowError(1, "Header row does not match the required template columns: "
                        + EXPECTED_HEADER));
                return new ImportResult(0, 0, 1, errors);
            }

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }
                total++;
                try {
                    materialMasterService.create(parseCsvRow(line));
                    succeeded++;
                } catch (DuplicateResourceException e) {
                    errors.add(new RowError(rowNumber, e.getMessage()));
                } catch (Exception e) {
                    errors.add(new RowError(rowNumber, "Invalid row data: " + e.getMessage()));
                }
            }
        }

        return new ImportResult(total, succeeded, total - succeeded, errors);
    }

    public ImportResult importFromExcel(MultipartFile file) throws IOException {
        List<RowError> errors = new ArrayList<>();
        int total = 0;
        int succeeded = 0;

        try (Workbook workbook = ExcelSheetReader.openWorkbook(file)) {
            Sheet sheet = ExcelSheetReader.requireFirstSheet(workbook);
            String header = ExcelSheetReader.headerLine(sheet, COLUMN_COUNT);
            if (!ExcelSheetReader.headersMatch(header, EXPECTED_HEADER)) {
                errors.add(new RowError(1, "Header row does not match the required template columns: "
                        + EXPECTED_HEADER + " (found: " + header + ")"));
                return new ImportResult(0, 0, 1, errors);
            }

            Iterator<Row> rows = ExcelSheetReader.dataRows(sheet);
            while (rows.hasNext()) {
                Row row = rows.next();
                int rowNumber = row.getRowNum() + 1; // Excel 1-based
                if (ExcelSheetReader.isBlankRow(row, COLUMN_COUNT)) {
                    continue;
                }
                total++;
                try {
                    materialMasterService.create(parseExcelRow(row));
                    succeeded++;
                } catch (DuplicateResourceException e) {
                    errors.add(new RowError(rowNumber, e.getMessage()));
                } catch (Exception e) {
                    errors.add(new RowError(rowNumber, "Invalid row data: " + e.getMessage()));
                }
            }
        }

        return new ImportResult(total, succeeded, total - succeeded, errors);
    }

    private CreateRequest parseCsvRow(String line) {
        String[] fields = line.split(",", -1);
        if (fields.length != COLUMN_COUNT) {
            throw new IllegalArgumentException("Expected " + COLUMN_COUNT + " columns, found " + fields.length);
        }
        return new CreateRequest(
                blankToNull(fields[0]),
                blankToNull(fields[1]),
                parseDecimal(fields[2], "rate"),
                blankToNull(fields[3]),
                blankToNull(fields[4]),
                parseDate(fields[5], "effectiveFrom"),
                null,
                blankToNull(fields[6]),
                blankToNull(fields[7]),
                parseDecimal(fields[8], "density"),
                fields[9].isBlank() ? null : parseDecimal(fields[9], "scrapRecoveryRate"),
                blankToNull(fields[10]),
                false,
                null,
                null,
                null
        );
    }

    private CreateRequest parseExcelRow(Row row) {
        return new CreateRequest(
                ExcelSheetReader.requireString(row, 0, "code"),
                ExcelSheetReader.optionalString(row, 1),
                ExcelSheetReader.requireDecimal(row, 2, "rate"),
                ExcelSheetReader.requireString(row, 3, "uom"),
                ExcelSheetReader.optionalString(row, 4),
                ExcelSheetReader.requireDate(row, 5, "effectiveFrom"),
                null,
                ExcelSheetReader.optionalString(row, 6),
                ExcelSheetReader.optionalString(row, 7),
                ExcelSheetReader.requireDecimal(row, 8, "density"),
                ExcelSheetReader.optionalDecimal(row, 9, "scrapRecoveryRate"),
                ExcelSheetReader.optionalString(row, 10),
                false,
                null,
                null,
                null
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal parseDecimal(String value, String fieldName) {
        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " is not a valid decimal: '" + value + "'");
        }
    }

    private LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " is not a valid ISO date (yyyy-MM-dd): '" + value + "'");
        }
    }
}
