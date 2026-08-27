package com.automotive.costing.material.controller;

import com.automotive.costing.platform.access.RequiresModule;

import com.automotive.costing.material.dto.CommodityIndexDtos;
import com.automotive.costing.material.dto.MaterialCostDtos.*;
import com.automotive.costing.material.service.CommodityIndexService;
import com.automotive.costing.material.service.MaterialCostService;
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
 * M03 Raw Material Cost Management. See TDD Section 5.03 / SRS Section 4.3.
 * User roles per SRS 4.3.7: Costing Engineer, Commodity/Material Buyer, Costing Manager
 * (approval - approval itself is exercised via MaterialMasterController's version-publish
 * flow, which already gates on rate-variance-triggered PENDING_APPROVAL status).
 */
@RestController
@RequestMapping("/api/v1/costing/material")
@RequiredArgsConstructor
@Tag(name = "Raw Material Cost Management")
@RequiresModule("PRODUCT_COSTING")
public class MaterialCostController {

    private final MaterialCostService materialCostService;
    private final CommodityIndexService commodityIndexService;

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COMMODITY_MATERIAL_BUYER')")
    @Operation(summary = "Calculate (and persist) material cost for a BOM node "
            + "(MAT-01, MAT-02, MAT-03, MAT-06); triggers a MAT-05 program roll-up")
    public ResponseEntity<Response> calculate(@Valid @RequestBody CalculateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materialCostService.calculate(request));
    }

    @PostMapping("/compare")
    @Operation(summary = "Compare material cost across two or more alternate grades/suppliers (MAT-04)")
    public ResponseEntity<CompareResponse> compare(@Valid @RequestBody CompareRequest request) {
        return ResponseEntity.ok(materialCostService.compareAlternates(request));
    }

    @GetMapping("/price-trend/{materialCode}")
    @Operation(summary = "Historical material price trend, up to 24 months (MAT-07)")
    public ResponseEntity<PriceTrendResponse> priceTrend(@PathVariable String materialCode) {
        return ResponseEntity.ok(materialCostService.priceTrend(materialCode));
    }

    @PostMapping("/high-cost-check")
    @Operation(summary = "Flag whether material cost exceeds a configurable % of total part cost (MAT-08)")
    public ResponseEntity<HighMaterialCostCheckResponse> highCostCheck(
            @Valid @RequestBody HighMaterialCostCheckRequest request) {
        return ResponseEntity.ok(materialCostService.checkHighMaterialCostRatio(request));
    }

    @PostMapping("/commodity-index")
    @PreAuthorize("hasAnyRole('MASTER_DATA_ADMIN', 'COMMODITY_MATERIAL_BUYER')")
    @Operation(summary = "Publish a dated commodity index value used by index-linked pricing (MAT-02)")
    public ResponseEntity<CommodityIndexDtos.Response> publishIndex(
            @Valid @RequestBody CommodityIndexDtos.PublishRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commodityIndexService.publish(request));
    }

    @GetMapping("/commodity-index/{indexCode}/history")
    @Operation(summary = "Publication history for a commodity index code (MAT-02)")
    public ResponseEntity<List<CommodityIndexDtos.Response>> indexHistory(@PathVariable String indexCode) {
        return ResponseEntity.ok(commodityIndexService.history(indexCode));
    }
}
