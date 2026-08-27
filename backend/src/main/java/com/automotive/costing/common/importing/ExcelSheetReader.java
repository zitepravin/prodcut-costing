package com.automotive.costing.common.importing;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Shared Apache POI helpers for .xlsx bulk import.
 * Cell values are normalized to strings via {@link DataFormatter} so Excel numeric/date
 * cells map cleanly onto the same parse rules used by CSV import services.
 */
public final class ExcelSheetReader {

    private static final DataFormatter FORMATTER = new DataFormatter(Locale.US);

    private ExcelSheetReader() {
    }

    public static boolean isExcelFilename(String filename) {
        if (filename == null) {
            return false;
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        return lower.endsWith(".xlsx") || lower.endsWith(".xls");
    }

    public static Workbook openWorkbook(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file is empty");
        }
        try (InputStream in = file.getInputStream()) {
            return WorkbookFactory.create(in);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to open Excel workbook. Provide a valid .xlsx file: " + e.getMessage(), e);
        }
    }

    public static Sheet requireFirstSheet(Workbook workbook) {
        if (workbook.getNumberOfSheets() < 1) {
            throw new IllegalArgumentException("Workbook contains no sheets");
        }
        return workbook.getSheetAt(0);
    }

    /**
     * Reads header cells from row 0 as comma-joined lowercase-comparable string
     * matching CSV template headers (trimmed, no extra spaces around commas).
     */
    public static String headerLine(Sheet sheet, int expectedColumns) {
        Row header = sheet.getRow(0);
        if (header == null) {
            throw new IllegalArgumentException("Header row (row 1) is missing");
        }
        List<String> cells = new ArrayList<>(expectedColumns);
        for (int c = 0; c < expectedColumns; c++) {
            cells.add(cellString(header, c));
        }
        return String.join(",", cells);
    }

    public static boolean headersMatch(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return actual.trim().equalsIgnoreCase(expected.trim());
    }

    public static String cellString(Row row, int columnIndex) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return "";
        }
        return FORMATTER.formatCellValue(cell).trim();
    }

    public static boolean isBlankRow(Row row, int columnCount) {
        if (row == null) {
            return true;
        }
        for (int c = 0; c < columnCount; c++) {
            if (!cellString(row, c).isBlank()) {
                return false;
            }
        }
        return true;
    }

    public static BigDecimal requireDecimal(Row row, int columnIndex, String fieldName) {
        String raw = cellString(row, columnIndex);
        if (raw.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        // DataFormatter may emit grouping commas or trailing .0 — normalize
        String normalized = raw.replace(",", "").trim();
        try {
            return new BigDecimal(normalized);
        } catch (Exception e) {
            // Fallback: true numeric cell
            Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                    && !DateUtil.isCellDateFormatted(cell)) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            throw new IllegalArgumentException(fieldName + " is not a valid decimal: '" + raw + "'");
        }
    }

    public static BigDecimal optionalDecimal(Row row, int columnIndex, String fieldName) {
        String raw = cellString(row, columnIndex);
        if (raw.isBlank()) {
            return null;
        }
        return requireDecimal(row, columnIndex, fieldName);
    }

    public static LocalDate requireDate(Row row, int columnIndex, String fieldName) {
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.of("UTC")).toLocalDate();
        }
        String raw = FORMATTER.formatCellValue(cell).trim();
        if (raw.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        // Accept ISO or common Excel display forms that DataFormatter emits
        try {
            if (raw.length() >= 10 && raw.charAt(4) == '-') {
                return LocalDate.parse(raw.substring(0, 10));
            }
            return LocalDate.parse(raw);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    fieldName + " is not a valid date (use yyyy-MM-dd): '" + raw + "'");
        }
    }

    public static LocalDate optionalDate(Row row, int columnIndex, String fieldName) {
        String raw = cellString(row, columnIndex);
        if (raw.isBlank()) {
            return null;
        }
        return requireDate(row, columnIndex, fieldName);
    }

    public static String requireString(Row row, int columnIndex, String fieldName) {
        String raw = cellString(row, columnIndex);
        if (raw.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return raw;
    }

    public static String optionalString(Row row, int columnIndex) {
        String raw = cellString(row, columnIndex);
        return raw.isBlank() ? null : raw;
    }

    public static Integer optionalInt(Row row, int columnIndex, String fieldName) {
        String raw = cellString(row, columnIndex);
        if (raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.replace(",", "").trim()).intValueExact();
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " is not a valid integer: '" + raw + "'");
        }
    }

    /** Iterate data rows starting at index 1 (row 2 in Excel). */
    public static Iterator<Row> dataRows(Sheet sheet) {
        Iterator<Row> it = sheet.iterator();
        if (it.hasNext()) {
            it.next(); // skip header
        }
        return it;
    }
}
