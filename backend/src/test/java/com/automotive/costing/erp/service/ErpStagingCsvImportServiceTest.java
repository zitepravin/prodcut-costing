package com.automotive.costing.erp.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.erp.dto.ErpDtos.BatchResponse;
import com.automotive.costing.erp.dto.ErpDtos.CsvIngestResult;
import com.automotive.costing.erp.dto.ErpDtos.IngestRequest;
import com.automotive.costing.erp.entity.ErpBatchStatus;
import com.automotive.costing.erp.entity.ErpBatchType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpStagingCsvImportServiceTest {

    @Mock ErpStagingService erpStagingService;

    ErpStagingCsvImportService service;

    @BeforeEach
    void setUp() {
        service = new ErpStagingCsvImportService(erpStagingService);
    }

    @Test
    @DisplayName("Import material prices CSV with partial success")
    void importMaterialPricesPartial() throws Exception {
        String csv = ErpStagingCsvImportService.MATERIAL_PRICE_HEADER + "\n"
                + "ST37,1.25,USD,2026-08-01,,,EA,\n"
                + ",bad,USD,2026-08-01,,,,\n"
                + "AL6061,3.50,EUR,2026-09-01,,,KG,\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "prices.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        when(erpStagingService.ingest(any())).thenReturn(sampleBatch(ErpBatchType.MATERIAL_PRICE, 2));

        CsvIngestResult result = service.importMaterialPrices(file, "SAP");

        assertEquals(3, result.totalRows());
        assertEquals(2, result.acceptedRows());
        assertEquals(1, result.rejectedRows());
        assertEquals(1, result.errors().size());

        ArgumentCaptor<IngestRequest> cap = ArgumentCaptor.forClass(IngestRequest.class);
        verify(erpStagingService).ingest(cap.capture());
        assertEquals(ErpBatchType.MATERIAL_PRICE, cap.getValue().batchType());
        assertEquals(2, cap.getValue().materialPrices().size());
    }

    @Test
    @DisplayName("Reject when no valid rows")
    void rejectAllInvalid() throws Exception {
        String csv = ErpStagingCsvImportService.MATERIAL_PRICE_HEADER + "\n"
                + ",,,,\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "bad.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        assertThrows(BusinessRuleViolationException.class,
                () -> service.importMaterialPrices(file, "CSV"));
    }

    @Test
    @DisplayName("Import parts CSV")
    void importParts() throws Exception {
        String csv = ErpStagingCsvImportService.PART_HEADER + "\n"
                + "EXT-1,PN-100,A,Bracket,1.2,EA,ST37,MAKE,P1,EV-CX-2027\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "parts.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        when(erpStagingService.ingest(any())).thenReturn(sampleBatch(ErpBatchType.PART, 1));

        CsvIngestResult result = service.importParts(file, "PLM");

        assertEquals(1, result.acceptedRows());
        ArgumentCaptor<IngestRequest> cap = ArgumentCaptor.forClass(IngestRequest.class);
        verify(erpStagingService).ingest(cap.capture());
        assertEquals("PN-100", cap.getValue().parts().get(0).partNumber());
    }

    @Test
    @DisplayName("Import BOM CSV")
    void importBom() throws Exception {
        String csv = ErpStagingCsvImportService.BOM_HEADER + "\n"
                + "ASM-ROOT,CHILD-1,2,A,B,EA,10,P1,EV-CX-2027\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "bom.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        when(erpStagingService.ingest(any())).thenReturn(sampleBatch(ErpBatchType.BOM, 1));

        CsvIngestResult result = service.importBom(file, "SAP");

        assertEquals(1, result.acceptedRows());
        ArgumentCaptor<IngestRequest> cap = ArgumentCaptor.forClass(IngestRequest.class);
        verify(erpStagingService).ingest(cap.capture());
        assertEquals("CHILD-1", cap.getValue().bomLines().get(0).childPartNumber());
    }

    private BatchResponse sampleBatch(ErpBatchType type, int rows) {
        return new BatchResponse(
                "b1", "CSV", type, ErpBatchStatus.RECEIVED, rows, 0,
                "file.csv", Instant.now(), null);
    }
}
