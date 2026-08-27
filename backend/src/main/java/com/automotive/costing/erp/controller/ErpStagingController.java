package com.automotive.costing.erp.controller;

import com.automotive.costing.erp.dto.ErpDtos.BatchDetailResponse;
import com.automotive.costing.erp.dto.ErpDtos.BatchResponse;
import com.automotive.costing.erp.dto.ErpDtos.CsvIngestResult;
import com.automotive.costing.erp.dto.ErpDtos.IngestRequest;
import com.automotive.costing.erp.dto.ErpDtos.PromoteResult;
import com.automotive.costing.erp.dto.ErpDtos.ValidateResult;
import com.automotive.costing.common.importing.ExcelSheetReader;
import com.automotive.costing.erp.service.ErpPromoteService;
import com.automotive.costing.erp.service.ErpStagingCsvImportService;
import com.automotive.costing.erp.service.ErpStagingExcelImportService;
import com.automotive.costing.erp.service.ErpStagingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.automotive.costing.platform.access.RequiresModule;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/erp/staging")
@RequiredArgsConstructor
@Tag(name = "ERP Staging")
@RequiresModule("PRODUCT_COSTING")
public class ErpStagingController {

    private final ErpStagingService erpStagingService;
    private final ErpPromoteService erpPromoteService;
    private final ErpStagingCsvImportService csvImportService;
    private final ErpStagingExcelImportService excelImportService;

    @PostMapping("/batches")
    @PreAuthorize("hasAnyRole('MASTER_DATA_ADMIN', 'ADMIN', 'SYSTEM_ADMINISTRATOR')")
    @Operation(summary = "Ingest ERP/PLM staging batch (parts, BOM lines, material prices)")
    public ResponseEntity<BatchResponse> ingest(@Valid @RequestBody IngestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(erpStagingService.ingest(request));
    }

    @PostMapping(value = "/batches/csv/material-prices", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MASTER_DATA_ADMIN', 'ADMIN', 'SYSTEM_ADMINISTRATOR')")
    @Operation(summary = "CSV or Excel (.xlsx) bulk ingest material prices into a new staging batch")
    public ResponseEntity<CsvIngestResult> importMaterialPrices(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceSystem", required = false) String sourceSystem) throws IOException {
        CsvIngestResult result = ExcelSheetReader.isExcelFilename(file.getOriginalFilename())
                ? excelImportService.importMaterialPrices(file, sourceSystem)
                : csvImportService.importMaterialPrices(file, sourceSystem);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping(value = "/batches/csv/parts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MASTER_DATA_ADMIN', 'ADMIN', 'SYSTEM_ADMINISTRATOR')")
    @Operation(summary = "CSV or Excel (.xlsx) bulk ingest parts into a new staging batch")
    public ResponseEntity<CsvIngestResult> importParts(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceSystem", required = false) String sourceSystem) throws IOException {
        CsvIngestResult result = ExcelSheetReader.isExcelFilename(file.getOriginalFilename())
                ? excelImportService.importParts(file, sourceSystem)
                : csvImportService.importParts(file, sourceSystem);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping(value = "/batches/csv/bom", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MASTER_DATA_ADMIN', 'ADMIN', 'SYSTEM_ADMINISTRATOR')")
    @Operation(summary = "CSV or Excel (.xlsx) bulk ingest BOM lines into a new staging batch")
    public ResponseEntity<CsvIngestResult> importBom(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceSystem", required = false) String sourceSystem) throws IOException {
        CsvIngestResult result = ExcelSheetReader.isExcelFilename(file.getOriginalFilename())
                ? excelImportService.importBom(file, sourceSystem)
                : csvImportService.importBom(file, sourceSystem);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/batches/{batchId}/validate")
    @PreAuthorize("hasAnyRole('MASTER_DATA_ADMIN', 'ADMIN', 'SYSTEM_ADMINISTRATOR')")
    @Operation(summary = "Validate staging rows; set batch READY or FAILED")
    public ResponseEntity<ValidateResult> validate(@PathVariable String batchId) {
        return ResponseEntity.ok(erpStagingService.validate(batchId));
    }

    @PostMapping("/batches/{batchId}/promote")
    @PreAuthorize("hasAnyRole('MASTER_DATA_ADMIN', 'ADMIN', 'SYSTEM_ADMINISTRATOR')")
    @Operation(summary = "Promote READY batch to live masters",
            description = "Material prices → MaterialMaster versions; parts acknowledged; "
                    + "BOM lines → BomNode tree when program_code resolves.")
    public ResponseEntity<PromoteResult> promote(@PathVariable String batchId) {
        return ResponseEntity.ok(erpPromoteService.promote(batchId));
    }

    @PostMapping("/batches/{batchId}/cancel")
    @PreAuthorize("hasAnyRole('MASTER_DATA_ADMIN', 'ADMIN', 'SYSTEM_ADMINISTRATOR')")
    @Operation(summary = "Cancel a non-promoted batch")
    public ResponseEntity<BatchResponse> cancel(@PathVariable String batchId) {
        return ResponseEntity.ok(erpStagingService.cancel(batchId));
    }

    @PostMapping("/batches/{batchId}/reopen")
    @PreAuthorize("hasAnyRole('MASTER_DATA_ADMIN', 'ADMIN', 'SYSTEM_ADMINISTRATOR')")
    @Operation(summary = "Re-open CANCELLED or FAILED batch to RECEIVED; reset row statuses")
    public ResponseEntity<BatchResponse> reopen(@PathVariable String batchId) {
        return ResponseEntity.ok(erpStagingService.reopen(batchId));
    }

    @GetMapping("/batches")
    @Operation(summary = "List inbound staging batches (newest first)")
    public ResponseEntity<List<BatchResponse>> list() {
        return ResponseEntity.ok(erpStagingService.listBatches());
    }

    @GetMapping("/batches/{batchId}")
    @Operation(summary = "Batch detail with part staging rows")
    public ResponseEntity<BatchDetailResponse> get(@PathVariable String batchId) {
        return ResponseEntity.ok(erpStagingService.getBatch(batchId));
    }
}
