package com.automotive.costing.masterdata.service;

import com.automotive.costing.common.exception.DuplicateResourceException;
import com.automotive.costing.common.importing.ExcelSheetReader;
import com.automotive.costing.masterdata.dto.BulkImportDtos.ImportResult;
import com.automotive.costing.masterdata.dto.BulkImportDtos.RowError;
import com.automotive.costing.masterdata.dto.MachineMasterDtos.CreateRequest;
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
 * Bulk import for machine masters — CSV and Excel (.xlsx), partial success (SRS 4.1.4).
 * Header (15 columns):
 * code,description,rate,uom,plantId,effectiveFrom,machineType,capacity,depreciationLifeYears,
 * capitalCost,annualOperatingHours,powerCostPerHour,maintenanceCostPerHour,floorSpaceCostPerHour,shiftPattern
 */
@Service
@RequiredArgsConstructor
public class MachineMasterBulkImportService {

    public static final String EXPECTED_HEADER =
            "code,description,rate,uom,plantId,effectiveFrom,machineType,capacity,depreciationLifeYears,"
                    + "capitalCost,annualOperatingHours,powerCostPerHour,maintenanceCostPerHour,"
                    + "floorSpaceCostPerHour,shiftPattern";
    private static final int COLUMN_COUNT = 15;

    private final MachineMasterService machineMasterService;

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
                errors.add(new RowError(1, "Header row does not match required columns: " + EXPECTED_HEADER));
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
                    machineMasterService.create(parseCsvRow(line));
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
                errors.add(new RowError(1, "Header row does not match required columns: "
                        + EXPECTED_HEADER + " (found: " + header + ")"));
                return new ImportResult(0, 0, 1, errors);
            }

            Iterator<Row> rows = ExcelSheetReader.dataRows(sheet);
            while (rows.hasNext()) {
                Row row = rows.next();
                int rowNumber = row.getRowNum() + 1;
                if (ExcelSheetReader.isBlankRow(row, COLUMN_COUNT)) {
                    continue;
                }
                total++;
                try {
                    machineMasterService.create(parseExcelRow(row));
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
        String[] c = line.split(",", -1);
        if (c.length < 6) {
            throw new IllegalArgumentException("Expected at least 6 columns");
        }
        return new CreateRequest(
                blankToNull(c[0]),
                blankToNull(c[1]),
                new BigDecimal(c[2].trim()),
                blankToNull(c[3]),
                blankToNull(c[4]),
                LocalDate.parse(c[5].trim()),
                null,
                c.length > 6 ? blankToNull(c[6]) : null,
                c.length > 7 ? blankToNull(c[7]) : null,
                optionalDecimal(c, 8),
                optionalDecimal(c, 9),
                optionalDecimal(c, 10),
                optionalDecimal(c, 11),
                optionalDecimal(c, 12),
                optionalDecimal(c, 13),
                c.length > 14 ? blankToNull(c[14]) : null
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
                ExcelSheetReader.optionalDecimal(row, 8, "depreciationLifeYears"),
                ExcelSheetReader.optionalDecimal(row, 9, "capitalCost"),
                ExcelSheetReader.optionalDecimal(row, 10, "annualOperatingHours"),
                ExcelSheetReader.optionalDecimal(row, 11, "powerCostPerHour"),
                ExcelSheetReader.optionalDecimal(row, 12, "maintenanceCostPerHour"),
                ExcelSheetReader.optionalDecimal(row, 13, "floorSpaceCostPerHour"),
                ExcelSheetReader.optionalString(row, 14)
        );
    }

    private static BigDecimal optionalDecimal(String[] c, int index) {
        if (c.length <= index || c[index].isBlank()) {
            return null;
        }
        return new BigDecimal(c[index].trim());
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
