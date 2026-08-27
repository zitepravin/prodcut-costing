package com.automotive.costing.quantitystructure.controller;

import com.automotive.costing.platform.access.RequiresModule;
import com.automotive.costing.quantitystructure.dto.QuantityStructureDtos.EstimateRequest;
import com.automotive.costing.quantitystructure.dto.QuantityStructureDtos.EstimateResponse;
import com.automotive.costing.quantitystructure.service.QuantityStructureCostingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/quantity-structure")
@RequiredArgsConstructor
@Tag(name = "Quantity-Structure Costing (A2)")
@RequiresModule("PRODUCT_COSTING")
public class QuantityStructureController {

    private final QuantityStructureCostingService service;

    @PostMapping("/estimate")
    @Operation(summary = "Automatic quantity-structure estimate from BOM + routing (+ optional costing variant)")
    public ResponseEntity<EstimateResponse> estimate(@Valid @RequestBody EstimateRequest request) {
        return ResponseEntity.ok(service.estimate(request));
    }
}
