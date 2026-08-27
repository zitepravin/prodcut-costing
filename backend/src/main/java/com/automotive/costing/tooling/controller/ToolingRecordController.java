package com.automotive.costing.tooling.controller;

import com.automotive.costing.platform.access.RequiresModule;

import com.automotive.costing.tooling.dto.ToolingRecordDtos.*;
import com.automotive.costing.tooling.service.ToolingRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * M06 Tooling, Die &amp; Fixture Investment Costing - record management. See TDD Section
 * 5.06 / SRS Section 4.6. Roles per SRS 4.6.7: Tooling Engineer, Costing Engineer,
 * Purchasing (tooling negotiation).
 */
@RestController
@RequestMapping("/api/v1/tooling")
@RequiredArgsConstructor
@Tag(name = "Tooling, Die & Fixture Investment Costing")
@RequiresModule("PRODUCT_COSTING")
public class ToolingRecordController {

    private final ToolingRecordService toolingRecordService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TOOLING_ENGINEER', 'COSTING_ENGINEER', 'PURCHASING_BUYER')")
    @Operation(summary = "Create a tooling investment record (TL-01)")
    public ResponseEntity<Response> create(@Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(toolingRecordService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a tooling record by ID")
    public ResponseEntity<Response> getById(@PathVariable String id) {
        return ResponseEntity.ok(toolingRecordService.getById(id));
    }

    @GetMapping("/by-bom-node/{bomNodeId}")
    @Operation(summary = "All tooling records for a BOM node, for TL-06 side-by-side comparison")
    public ResponseEntity<List<Response>> getByBomNode(@PathVariable String bomNodeId) {
        return ResponseEntity.ok(toolingRecordService.getByBomNode(bomNodeId));
    }

    @GetMapping("/assets")
    @Operation(summary = "Tooling asset register, optionally filtered by plant location (TL-07)")
    public ResponseEntity<List<Response>> assetRegister(
            @RequestParam(required = false) String plantLocation) {
        return ResponseEntity.ok(toolingRecordService.assetRegister(plantLocation));
    }

    @PutMapping("/{id}/retire")
    @PreAuthorize("hasAnyRole('TOOLING_ENGINEER', 'COSTING_ENGINEER')")
    @Operation(summary = "Retire a tool - never deleted, kept in the asset register (TL-07)")
    public ResponseEntity<Response> retire(@PathVariable String id, @RequestBody(required = false) RetireRequest request) {
        return ResponseEntity.ok(toolingRecordService.retire(id, request == null ? new RetireRequest(null) : request));
    }
}
