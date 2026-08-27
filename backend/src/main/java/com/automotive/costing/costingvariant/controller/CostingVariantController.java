package com.automotive.costing.costingvariant.controller;

import com.automotive.costing.costingvariant.dto.CostingVariantDtos.*;
import com.automotive.costing.costingvariant.service.CostingVariantService;
import com.automotive.costing.platform.access.RequiresModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/costing-variants")
@RequiredArgsConstructor
@Tag(name = "Costing Variant Engine (A1)")
@RequiresModule("PRODUCT_COSTING")
public class CostingVariantController {

    private final CostingVariantService service;

    @GetMapping
    @Operation(summary = "List all costing variants")
    public ResponseEntity<List<VariantResponse>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/active")
    @Operation(summary = "List ACTIVE variants eligible for estimates")
    public ResponseEntity<List<VariantResponse>> listActive() {
        return ResponseEntity.ok(service.listActive());
    }

    @GetMapping("/{variantId}")
    public ResponseEntity<VariantResponse> get(@PathVariable String variantId) {
        return ResponseEntity.ok(service.get(variantId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'MASTER_DATA_ADMIN', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Create DRAFT costing variant")
    public ResponseEntity<VariantResponse> create(@Valid @RequestBody CreateVariantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{variantId}")
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'MASTER_DATA_ADMIN', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<VariantResponse> update(
            @PathVariable String variantId,
            @Valid @RequestBody UpdateVariantRequest request) {
        return ResponseEntity.ok(service.update(variantId, request));
    }

    @PostMapping("/{variantId}/activate")
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<VariantResponse> activate(@PathVariable String variantId) {
        return ResponseEntity.ok(service.activate(variantId));
    }

    @PostMapping("/{variantId}/retire")
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<VariantResponse> retire(@PathVariable String variantId) {
        return ResponseEntity.ok(service.retire(variantId));
    }
}
