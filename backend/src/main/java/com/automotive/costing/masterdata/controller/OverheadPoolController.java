package com.automotive.costing.masterdata.controller;

import com.automotive.costing.platform.access.RequiresModule;

import com.automotive.costing.masterdata.dto.OverheadPoolDtos.CreateRequest;
import com.automotive.costing.masterdata.dto.OverheadPoolDtos.RecalculateRequest;
import com.automotive.costing.masterdata.dto.OverheadPoolDtos.Response;
import com.automotive.costing.masterdata.service.OverheadPoolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** M01/M05 - Overhead Pool configuration. See API_SPEC.md Section 7 / TDD Section 5.05. */
@RestController
@RequestMapping("/api/v1/rates/overhead/pools")
@RequiredArgsConstructor
@Tag(name = "Master Data - Overhead Pools")
@RequiresModule("PRODUCT_COSTING")
public class OverheadPoolController {

    private final OverheadPoolService service;

    @PostMapping
    @PreAuthorize("hasRole('FINANCE_CONTROLLER')")
    @Operation(summary = "Configure an overhead pool (OHD-02: at least 4 required with independent allocation bases)")
    public ResponseEntity<Response> create(@Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PostMapping("/{code}/recalculate")
    @PreAuthorize("hasRole('FINANCE_CONTROLLER')")
    @Operation(summary = "Recalculate absorption rate for an updated allocation base (OHD-03)")
    public ResponseEntity<Response> recalculate(@PathVariable String code,
                                                  @Valid @RequestBody RecalculateRequest request) {
        return ResponseEntity.ok(service.recalculate(code, request));
    }

    @GetMapping("/compliance-check")
    @Operation(summary = "Check whether the minimum 4 required overhead pool categories are configured (OHD-02)")
    public ResponseEntity<Boolean> complianceCheck() {
        return ResponseEntity.ok(service.hasMinimumRequiredPools());
    }
}
