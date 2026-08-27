package com.automotive.costing.procurement.controller;

import com.automotive.costing.platform.access.RequiresModule;
import com.automotive.costing.procurement.dto.ProcurementDtos.*;
import com.automotive.costing.procurement.service.ProcurementAlternativeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/procurement-alternatives")
@RequiredArgsConstructor
@Tag(name = "Procurement Alternatives (B6)")
@RequiresModule("PRODUCT_COSTING")
public class ProcurementAlternativeController {

    private final ProcurementAlternativeService service;

    @GetMapping
    public ResponseEntity<List<SetResponse>> list(@RequestParam String bomNodeId) {
        return ResponseEntity.ok(service.listByBomNode(bomNodeId));
    }

    @GetMapping("/{setId}")
    public ResponseEntity<SetResponse> get(@PathVariable String setId) {
        return ResponseEntity.ok(service.get(setId));
    }

    @PostMapping("/evaluate")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'BUYER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Build MAKE/BUY/SUBCONTRACT/EXTERNAL/MIXED alternative set and rank by unit cost")
    public ResponseEntity<SetResponse> evaluate(@Valid @RequestBody EvaluateRequest request) {
        return ResponseEntity.ok(service.evaluate(request));
    }

    @PostMapping("/{setId}/decide")
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'BUYER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Human decision on alternative set — does not auto-award")
    public ResponseEntity<SetResponse> decide(
            @PathVariable String setId,
            @Valid @RequestBody DecisionRequest request) {
        return ResponseEntity.ok(service.decide(setId, request));
    }
}
