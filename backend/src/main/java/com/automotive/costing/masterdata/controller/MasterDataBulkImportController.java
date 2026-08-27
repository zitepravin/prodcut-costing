package com.automotive.costing.masterdata.controller;

import com.automotive.costing.masterdata.dto.BulkImportDtos.ImportResult;
import com.automotive.costing.masterdata.service.LaborMasterBulkImportService;
import com.automotive.costing.masterdata.service.MachineMasterBulkImportService;
import com.automotive.costing.masterdata.service.MaterialMasterBulkImportService;
import com.automotive.costing.masterdata.service.MasterDataExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.automotive.costing.platform.access.RequiresModule;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** MDM bulk import + CSV export. Materials existed; machines/labor/export expand the surface. */
@RestController
@RequestMapping("/api/v1/master")
@RequiredArgsConstructor
@Tag(name = "Master Data - Bulk Import / Export")
@RequiresModule("PRODUCT_COSTING")
public class MasterDataBulkImportController {

    private final MaterialMasterBulkImportService materialBulkImportService;
    private final MachineMasterBulkImportService machineBulkImportService;
    private final LaborMasterBulkImportService laborBulkImportService;
    private final MasterDataExportService exportService;

    @PostMapping(value = "/bulk-import/materials", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('MASTER_DATA_ADMIN')")
    @Operation(summary = "Bulk import material masters from CSV or Excel .xlsx (partial success)")
    public ResponseEntity<ImportResult> importMaterials(@RequestParam("file") MultipartFile file)
            throws IOException {
        return ResponseEntity.ok(materialBulkImportService.importFile(file));
    }

    @PostMapping(value = "/bulk-import/machines", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('MASTER_DATA_ADMIN')")
    @Operation(summary = "Bulk import machine masters from CSV or Excel .xlsx (partial success)")
    public ResponseEntity<ImportResult> importMachines(@RequestParam("file") MultipartFile file)
            throws IOException {
        return ResponseEntity.ok(machineBulkImportService.importFile(file));
    }

    @PostMapping(value = "/bulk-import/labor", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('MASTER_DATA_ADMIN')")
    @Operation(summary = "Bulk import labor masters from CSV or Excel .xlsx (partial success)")
    public ResponseEntity<ImportResult> importLabor(@RequestParam("file") MultipartFile file)
            throws IOException {
        return ResponseEntity.ok(laborBulkImportService.importFile(file));
    }

    @GetMapping(value = "/export/materials", produces = "text/csv")
    @Operation(summary = "Export active material masters as CSV (import-compatible header)")
    public ResponseEntity<byte[]> exportMaterials() {
        return csvAttachment("materials.csv", exportService.exportMaterialsCsv());
    }

    @GetMapping(value = "/export/machines", produces = "text/csv")
    @Operation(summary = "Export active machine masters as CSV (import-compatible header)")
    public ResponseEntity<byte[]> exportMachines() {
        return csvAttachment("machines.csv", exportService.exportMachinesCsv());
    }

    @GetMapping(value = "/export/labor", produces = "text/csv")
    @Operation(summary = "Export active labor masters as CSV (import-compatible header)")
    public ResponseEntity<byte[]> exportLabor() {
        return csvAttachment("labor.csv", exportService.exportLaborCsv());
    }

    private static ResponseEntity<byte[]> csvAttachment(String filename, String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(bytes);
    }
}
