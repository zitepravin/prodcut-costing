package com.automotive.costing.masterdata.service;

import com.automotive.costing.masterdata.dto.BulkImportDtos.ImportResult;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MachineLaborExcelImportTest {

    @Mock MachineMasterService machineMasterService;
    @Mock LaborMasterService laborMasterService;

    MachineMasterBulkImportService machineImport;
    LaborMasterBulkImportService laborImport;

    @BeforeEach
    void setUp() {
        machineImport = new MachineMasterBulkImportService(machineMasterService);
        laborImport = new LaborMasterBulkImportService(laborMasterService);
    }

    @Test
    @DisplayName("Machine Excel import partial success")
    void machineExcelPartial() throws Exception {
        byte[] bytes = workbook(MachineMasterBulkImportService.EXPECTED_HEADER, row -> {
            row.createCell(0).setCellValue("CNC-01");
            row.createCell(1).setCellValue("CNC Mill");
            row.createCell(2).setCellValue(85.0);
            row.createCell(3).setCellValue("HR");
            row.createCell(4).setCellValue("P1");
            row.createCell(5).setCellValue("2026-08-01");
            row.createCell(6).setCellValue("CNC");
        }, row -> {
            row.createCell(0).setCellValue("BAD");
            // missing rate/uom/effectiveFrom
        });

        when(machineMasterService.create(any())).thenReturn(null);

        MockMultipartFile file = new MockMultipartFile(
                "file", "machines.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bytes);

        ImportResult result = machineImport.importFromExcel(file);
        assertEquals(2, result.totalRows());
        assertEquals(1, result.succeeded());
        assertEquals(1, result.failed());
        verify(machineMasterService, times(1)).create(any());
    }

    @Test
    @DisplayName("Labor Excel import success")
    void laborExcelSuccess() throws Exception {
        byte[] bytes = workbook(LaborMasterBulkImportService.EXPECTED_HEADER, row -> {
            row.createCell(0).setCellValue("OP-01");
            row.createCell(1).setCellValue("Operator");
            row.createCell(2).setCellValue(28.5);
            row.createCell(3).setCellValue("HR");
            row.createCell(4).setCellValue("P1");
            row.createCell(5).setCellValue("2026-08-01");
            row.createCell(6).setCellValue("SKILLED");
            row.createCell(7).setCellValue("DAY");
            row.createCell(8).setCellValue("US");
            row.createCell(9).setCellValue(0.35);
        });

        when(laborMasterService.create(any())).thenReturn(null);

        MockMultipartFile file = new MockMultipartFile(
                "file", "labor.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bytes);

        ImportResult result = laborImport.importFromExcel(file);
        assertEquals(1, result.totalRows());
        assertEquals(1, result.succeeded());
        verify(laborMasterService, times(1)).create(any());
    }

    @FunctionalInterface
    interface RowFiller {
        void fill(Row row);
    }

    private byte[] workbook(String headerCsv, RowFiller... fillers) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            String[] cols = headerCsv.split(",");
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }
            int r = 1;
            for (RowFiller f : fillers) {
                Row row = sheet.createRow(r++);
                f.fill(row);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }
}
