package com.automotive.costing.masterdata.controller;

import com.automotive.costing.platform.access.RequiresModule;

import com.automotive.costing.masterdata.dto.MaterialMasterDtos.CreateRequest;
import com.automotive.costing.masterdata.dto.MaterialMasterDtos.Response;
import com.automotive.costing.masterdata.dto.MaterialMasterDtos.UpdateRequest;
import com.automotive.costing.masterdata.entity.MasterDataStatus;
import com.automotive.costing.masterdata.service.MaterialMasterService;
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

import java.util.List;

/**
 * M01 Master Data Management - Material Master endpoints.
 * See API_SPEC.md Section 3. Controllers are thin: validate input shape (via @Valid),
 * delegate to the service, map the HTTP status. No business logic lives here.
 */
@RestController
@RequestMapping("/api/v1/master/materials")
@RequiredArgsConstructor
@Tag(name = "Master Data - Materials")
@RequiresModule("PRODUCT_COSTING")
public class MaterialMasterController {

    private final MaterialMasterService service;

    @GetMapping
    @Operation(summary = "List/search material master records")
    public ResponseEntity<Page<Response>> search(
            @RequestParam(required = false) MasterDataStatus status,
            @RequestParam(required = false) String plantId,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(status, plantId, q, pageable));
    }

    @GetMapping("/by-code/{code}/versions")
    @Operation(summary = "Version history for a material code (MDM-02), newest first")
    public ResponseEntity<List<Response>> versions(@PathVariable String code) {
        return ResponseEntity.ok(service.listVersionsByCode(code));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a material master record by ID")
    public ResponseEntity<Response> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('MASTER_DATA_ADMIN')")
    @Operation(summary = "Create a new material master record (MDM-01, MDM-03)")
    public ResponseEntity<Response> create(@Valid @RequestBody CreateRequest request) {
        Response created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{code}/new-version")
    @PreAuthorize("hasRole('MASTER_DATA_ADMIN')")
    @Operation(summary = "Publish a new version of an existing material master record (MDM-02). "
            + "This never edits the existing row in place.")
    public ResponseEntity<Response> createNewVersion(@PathVariable String code,
                                                       @Valid @RequestBody UpdateRequest request) {
        return ResponseEntity.ok(service.createNewVersion(code, request));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Approve a PENDING_APPROVAL material version; closes prior ACTIVE effective window")
    public ResponseEntity<Response> approve(@PathVariable String id) {
        return ResponseEntity.ok(service.approve(id));
    }

    @GetMapping("/by-code/{code}/effective")
    @Operation(summary = "Resolve material rate effective as of a valuation date (historical costing)")
    public ResponseEntity<Response> effectiveAsOf(
            @PathVariable String code,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate asOf) {
        return ResponseEntity.ok(service.findEffectiveAsOf(code, asOf));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('MASTER_DATA_ADMIN')")
    @Operation(summary = "Deactivate a material master record. This is the only retirement "
            + "path - records referenced by an approved cost estimate can never be deleted.")
    public ResponseEntity<Response> deactivate(@PathVariable String id) {
        return ResponseEntity.ok(service.deactivate(id));
    }
}
