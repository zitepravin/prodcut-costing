package com.automotive.costing.erp.sap.controller;

import com.automotive.costing.erp.sap.dto.SapCostDtos.CompareRequest;
import com.automotive.costing.erp.sap.dto.SapCostDtos.CompareResponse;
import com.automotive.costing.erp.sap.dto.SapCostDtos.LandRequest;
import com.automotive.costing.erp.sap.dto.SapCostDtos.MappingResponse;
import com.automotive.costing.erp.sap.dto.SapCostDtos.MappingUpsert;
import com.automotive.costing.erp.sap.dto.SapCostDtos.StagingResponse;
import com.automotive.costing.erp.sap.service.SapCostEstimateService;
import com.automotive.costing.platform.access.RequiresModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contract-first SAP CO cost-estimate landing zone.
 * Customer CPI posts BAPI-shaped JSON here — no direct RFC from this service.
 */
@RestController
@RequestMapping("/api/v1/erp/sap")
@RequiredArgsConstructor
@Tag(name = "SAP CO Cost Estimate Integration")
@RequiresModule("PRODUCT_COSTING")
public class SapCostEstimateController {

    private final SapCostEstimateService service;

    @PostMapping("/cost-estimates")
    @Operation(summary = "Land a SAP cost estimate (from CPI / middleware)")
    public ResponseEntity<StagingResponse> land(@Valid @RequestBody LandRequest request) {
        return ResponseEntity.ok(service.land(request));
    }

    @GetMapping("/cost-estimates")
    @Operation(summary = "List landed SAP cost estimates")
    public ResponseEntity<List<StagingResponse>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/cost-estimates/{stagingId}")
    @Operation(summary = "Get one staged estimate")
    public ResponseEntity<StagingResponse> get(@PathVariable String stagingId) {
        return ResponseEntity.ok(service.get(stagingId));
    }

    @PostMapping("/cost-estimates/{stagingId}/map-components")
    @Operation(summary = "Refresh SAP→our component mapping on staged rows")
    public ResponseEntity<StagingResponse> remap(@PathVariable String stagingId) {
        return ResponseEntity.ok(service.remap(stagingId));
    }

    @PostMapping("/cost-estimates/{stagingId}/compare-should-cost")
    @Operation(summary = "Compare staged SAP estimate to a should-cost build-up")
    public ResponseEntity<CompareResponse> compare(
            @PathVariable String stagingId,
            @Valid @RequestBody CompareRequest request
    ) {
        return ResponseEntity.ok(service.compare(stagingId, request));
    }

    @GetMapping("/component-mappings")
    @Operation(summary = "List active SAP→our cost component mappings")
    public ResponseEntity<List<MappingResponse>> listMappings() {
        return ResponseEntity.ok(service.listMappings());
    }

    @PutMapping("/component-mappings")
    @Operation(summary = "Create or update a component mapping")
    public ResponseEntity<MappingResponse> upsertMapping(@Valid @RequestBody MappingUpsert request) {
        return ResponseEntity.ok(service.upsertMapping(request));
    }
}
