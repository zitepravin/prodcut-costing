package com.automotive.costing.erp.service;

import com.automotive.costing.erp.dto.ErpDtos.BatchResponse;
import com.automotive.costing.erp.dto.ErpDtos.CsvIngestResult;
import com.automotive.costing.erp.dto.ErpDtos.IngestRequest;
import com.automotive.costing.erp.entity.ErpBatchStatus;
import com.automotive.costing.erp.entity.ErpBatchType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpStagingExcelImportServiceTest {

    @Mock ErpStagingService erpStagingService;

    ErpStagingExcelImportService service;

    @BeforeEach
    void setUp() {
        service = new ErpStagingExcelImportService(erpStagingService);
    }

    @Test
    @DisplayName("Excel material prices partial success")
    void materialPricesPartial() throws Exception {
        byte[] bytes;
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet();
            Row h = sheet.createRow(0);
            String[] cols = ErpStagingCsvImportService.MATERIAL_PRICE_HEADER.split(",");
            for (int i = 0; i < cols.length; i++) {
                h.createCell(i).setCellValue(cols[i]);
            }
            Row good = sheet.createRow(1);
            good.createCell(0).setCellValue("ST37");
            good.createCell(1).setCellValue(1.25);
            good.createCell(2).setCellValue("USD");
            good.createCell(3).setCellValue("2026-08-01");
            Row bad = sheet.createRow(2);
            bad.createCell(2).setCellValue("USD");
            wb.write(out);
            bytes = out.toByteArray();
        }

        when(erpStagingService.ingest(any())).thenReturn(new BatchResponse(
                "b1", "EXCEL", ErpBatchType.MATERIAL_PRICE, ErpBatchStatus.RECEIVED,
                1, 0, "prices.xlsx", Instant.now(), null));

        MockMultipartFile file = new MockMultipartFile(
                "file", "prices.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bytes);

        CsvIngestResult result = service.importMaterialPrices(file, "SAP");

        assertEquals(2, result.totalRows());
        assertEquals(1, result.acceptedRows());
        assertEquals(1, result.rejectedRows());
        ArgumentCaptor<IngestRequest> cap = ArgumentCaptor.forClass(IngestRequest.class);
        verify(erpStagingService).ingest(cap.capture());
        assertEquals(1, cap.getValue().materialPrices().size());
        assertEquals("ST37", cap.getValue().materialPrices().get(0).materialCode());
    }
}
