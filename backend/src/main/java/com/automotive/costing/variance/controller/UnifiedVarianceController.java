package com.automotive.costing.variance.controller;

import com.automotive.costing.platform.access.RequiresModule;
import com.automotive.costing.variance.dto.VarianceDtos.CompareRequest;
import com.automotive.costing.variance.dto.VarianceDtos.CompareResponse;
import com.automotive.costing.variance.service.UnifiedVarianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/variance")
@RequiredArgsConstructor
@Tag(name = "Unified Variance Engine (B3)")
@RequiresModule("PRODUCT_COSTING")
public class UnifiedVarianceController {

    private final UnifiedVarianceService service;

    @PostMapping("/compare")
    @Operation(summary = "Compare cost snapshots with driver-level variance")
    public ResponseEntity<CompareResponse> compare(@Valid @RequestBody CompareRequest request) {
        return ResponseEntity.ok(service.compare(request));
    }
}
