package com.automotive.costing.costingsheet.controller;

import com.automotive.costing.costingsheet.dto.CostingSheetDtos.*;
import com.automotive.costing.costingsheet.service.CostingSheetService;
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
@RequestMapping("/api/v1/costing-sheets")
@RequiredArgsConstructor
@Tag(name = "Costing Sheet Engine (A6)")
@RequiresModule("PRODUCT_COSTING")
public class CostingSheetController {

    private final CostingSheetService service;

    @GetMapping
    public ResponseEntity<List<SheetResponse>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{sheetId}")
    public ResponseEntity<SheetDetailResponse> get(@PathVariable String sheetId) {
        return ResponseEntity.ok(service.get(sheetId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'MASTER_DATA_ADMIN', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<SheetResponse> create(@Valid @RequestBody CreateSheetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSheet(request));
    }

    @PostMapping("/{sheetId}/rules")
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'MASTER_DATA_ADMIN', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<RuleResponse> addRule(
            @PathVariable String sheetId,
            @Valid @RequestBody CreateRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addRule(sheetId, request));
    }

    @PostMapping("/{sheetId}/activate")
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<SheetResponse> activate(@PathVariable String sheetId) {
        return ResponseEntity.ok(service.activate(sheetId));
    }

    @PostMapping("/{sheetId}/retire")
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<SheetResponse> retire(@PathVariable String sheetId) {
        return ResponseEntity.ok(service.retire(sheetId));
    }

    @PostMapping("/apply")
    @Operation(summary = "Apply ACTIVE costing sheet to a cost base (audited overhead lines)")
    public ResponseEntity<ApplyResponse> apply(@Valid @RequestBody ApplyRequest request) {
        return ResponseEntity.ok(service.apply(request));
    }
}
