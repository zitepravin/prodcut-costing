package com.automotive.costing.reports.controller;

import com.automotive.costing.reports.dto.ReportDtos.ErpStagingReport;
import com.automotive.costing.reports.dto.ReportDtos.MasterDataHealthReport;
import com.automotive.costing.reports.dto.ReportDtos.OperationalReport;
import com.automotive.costing.reports.dto.ReportDtos.ShouldCostElementReport;
import com.automotive.costing.reports.dto.ReportDtos.TargetVarianceReport;
import com.automotive.costing.reports.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.automotive.costing.platform.access.RequiresModule;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports")
@RequiresModule("PRODUCT_COSTING")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/operational")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Composite operational report; optional programId scopes BOM / should-cost / variance")
    public ResponseEntity<OperationalReport> operational(
            @RequestParam(value = "programId", required = false) String programId) {
        return ResponseEntity.ok(reportService.operational(programId));
    }

    @GetMapping(value = "/operational/export", produces = "text/csv")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "CSV export of operational report (section,metric,value)")
    public ResponseEntity<byte[]> operationalExport(
            @RequestParam(value = "programId", required = false) String programId) {
        String csv = reportService.operationalCsv(programId);
        String filename = programId == null || programId.isBlank()
                ? "operational-report-all.csv"
                : "operational-report-" + programId + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/master-data-health")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Master data volume and ACTIVE version counts (system-wide)")
    public ResponseEntity<MasterDataHealthReport> masterDataHealth() {
        return ResponseEntity.ok(reportService.masterDataHealth());
    }

    @GetMapping("/erp-staging")
    @PreAuthorize("hasAnyRole('MASTER_DATA_ADMIN', 'ADMIN', 'SYSTEM_ADMINISTRATOR')")
    @Operation(summary = "ERP inbound batch counts by status")
    public ResponseEntity<ErpStagingReport> erpStaging() {
        return ResponseEntity.ok(reportService.erpStaging());
    }

    @GetMapping("/should-cost-elements")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Should-cost element totals; optional programId filter")
    public ResponseEntity<ShouldCostElementReport> shouldCostElements(
            @RequestParam(value = "programId", required = false) String programId) {
        return ResponseEntity.ok(reportService.shouldCostElements(programId));
    }

    @GetMapping("/target-variance")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Target-cost variance KPIs; optional programId filter")
    public ResponseEntity<TargetVarianceReport> targetVariance(
            @RequestParam(value = "programId", required = false) String programId) {
        return ResponseEntity.ok(reportService.targetVariance(programId));
    }
}
