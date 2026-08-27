package com.automotive.costing.masterdata.service;

import com.automotive.costing.masterdata.dto.BulkImportDtos.ImportResult;
import com.automotive.costing.masterdata.dto.MaterialMasterDtos.CreateRequest;
import com.automotive.costing.masterdata.dto.MaterialMasterDtos.Response;
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
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialMasterExcelImportTest {

    @Mock MaterialMasterService materialMasterService;

    MaterialMasterBulkImportService service;

    @BeforeEach
    void setUp() {
        service = new MaterialMasterBulkImportService(materialMasterService);
    }

    @Test
    @DisplayName("Excel import partial success with same header as CSV")
    void excelPartialSuccess() throws Exception {
        byte[] bytes;
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            String[] cols = MaterialMasterBulkImportService.EXPECTED_HEADER.split(",");
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }
            // good row
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("ST37");
            r1.createCell(1).setCellValue("Steel");
            r1.createCell(2).setCellValue(1.25);
            r1.createCell(3).setCellValue("KG");
            r1.createCell(4).setCellValue("P1");
            r1.createCell(5).setCellValue("2026-08-01");
            r1.createCell(6).setCellValue("A");
            r1.createCell(7).setCellValue("DIN");
            r1.createCell(8).setCellValue(7.85);
            r1.createCell(9).setCellValue(0.05);
            r1.createCell(10).setCellValue("sheet");
            // bad row — missing rate
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("BAD");
            r2.createCell(3).setCellValue("KG");
            r2.createCell(5).setCellValue("2026-08-01");
            r2.createCell(8).setCellValue(1.0);
            wb.write(out);
            bytes = out.toByteArray();
        }

        when(materialMasterService.create(any(CreateRequest.class))).thenAnswer(inv -> {
            CreateRequest req = inv.getArgument(0);
            return new Response(
                    "id-1", req.code(), req.description(), req.rate(), req.uom(), req.plantId(),
                    req.effectiveFrom(), null, 1, null, req.grade(), req.specificationStandard(),
                    req.density(), req.scrapRecoveryRate(), req.materialForm(),
                    false, null, null, null, "test", null, null, null);
        });

        MockMultipartFile file = new MockMultipartFile(
                "file", "materials.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bytes);

        ImportResult result = service.importFromExcel(file);

        assertEquals(2, result.totalRows());
        assertEquals(1, result.succeeded());
        assertEquals(1, result.failed());
        verify(materialMasterService, times(1)).create(any());
    }

    @Test
    @DisplayName("importFile routes .xlsx to Excel path")
    void importFileRoutesExcel() throws Exception {
        byte[] bytes;
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            String[] cols = MaterialMasterBulkImportService.EXPECTED_HEADER.split(",");
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }
            wb.write(out);
            bytes = out.toByteArray();
        }
        MockMultipartFile file = new MockMultipartFile(
                "file", "m.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bytes);
        ImportResult result = service.importFile(file);
        assertEquals(0, result.totalRows());
    }
}
