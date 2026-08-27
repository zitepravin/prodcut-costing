package com.automotive.costing.masterdata.controller;

import com.automotive.costing.platform.access.RequiresModule;

import com.automotive.costing.masterdata.dto.LaborMasterDtos.CreateRequest;
import com.automotive.costing.masterdata.dto.LaborMasterDtos.Response;
import com.automotive.costing.masterdata.dto.LaborMasterDtos.UpdateRequest;
import com.automotive.costing.masterdata.entity.MasterDataStatus;
import com.automotive.costing.masterdata.service.LaborMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** M01 - Labor Master. See API_SPEC.md Section 3. */
@RestController
@RequestMapping("/api/v1/master/labor")
@RequiredArgsConstructor
@Tag(name = "Master Data - Labor")
@RequiresModule("PRODUCT_COSTING")
public class LaborMasterController {

    private final LaborMasterService service;

    @GetMapping
    @Operation(summary = "List/search labor master records")
    public ResponseEntity<Page<Response>> search(
            @RequestParam(required = false) MasterDataStatus status,
            @RequestParam(required = false) String plantId,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(status, plantId, q, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a labor master record by ID")
    public ResponseEntity<Response> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('MASTER_DATA_ADMIN')")
    @Operation(summary = "Create a new labor master record")
    public ResponseEntity<Response> create(@Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PostMapping("/{code}/new-version")
    @PreAuthorize("hasRole('MASTER_DATA_ADMIN')")
    @Operation(summary = "Publish a new version of an existing labor master record")
    public ResponseEntity<Response> createNewVersion(@PathVariable String code,
                                                       @Valid @RequestBody UpdateRequest request) {
        return ResponseEntity.ok(service.createNewVersion(code, request));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Approve a PENDING_APPROVAL version; closes prior ACTIVE effective window (V227)")
    public ResponseEntity<Response> approve(@PathVariable String id) {
        return ResponseEntity.ok(service.approve(id));
    }

    @GetMapping("/by-code/{code}/effective")
    @Operation(summary = "Resolve rate effective as of a valuation date (historical costing)")
    public ResponseEntity<Response> effectiveAsOf(
            @PathVariable String code,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate asOf) {
        return ResponseEntity.ok(service.findEffectiveAsOf(code, asOf));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('MASTER_DATA_ADMIN')")
    @Operation(summary = "Deactivate a labor master record")
    public ResponseEntity<Response> deactivate(@PathVariable String id) {
        return ResponseEntity.ok(service.deactivate(id));
    }
}
