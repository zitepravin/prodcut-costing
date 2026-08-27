package com.automotive.costing.shouldcost.controller;

import com.automotive.costing.platform.access.RequiresModule;

import com.automotive.costing.shouldcost.dto.BenchmarkDtos.*;
import com.automotive.costing.shouldcost.service.BenchmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** M08 reference-data management: SG&amp;A/margin benchmarks (SRS 4.8.5) and SC-02's parametric $/kg benchmarks. */
@RestController
@RequestMapping("/api/v1/should-cost/benchmarks")
@RequiredArgsConstructor
@Tag(name = "Should-Cost Benchmarks")
@RequiresModule("PRODUCT_COSTING")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    @PostMapping("/sga-margin")
    @PreAuthorize("hasAnyRole('MASTER_DATA_ADMIN', 'FINANCE_CONTROLLER')")
    @Operation(summary = "Configure an SG&A/margin benchmark by commodity family and region (SRS 4.8.5)")
    public ResponseEntity<SgaMarginResponse> createSgaMargin(@Valid @RequestBody SgaMarginCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(benchmarkService.createSgaMarginBenchmark(request));
    }

    @PostMapping("/parametric")
    @PreAuthorize("hasAnyRole('MASTER_DATA_ADMIN', 'FINANCE_CONTROLLER')")
    @Operation(summary = "Configure a parametric $/kg cost benchmark (SC-02)")
    public ResponseEntity<ParametricResponse> createParametric(@Valid @RequestBody ParametricCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(benchmarkService.createParametricBenchmark(request));
    }
}
