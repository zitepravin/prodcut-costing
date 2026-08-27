package com.automotive.costing.shouldcost.controller;

import com.automotive.costing.platform.access.RequiresModule;

import com.automotive.costing.shouldcost.dto.ShouldCostDtos.*;
import com.automotive.costing.shouldcost.service.ShouldCostService;
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
 * M08 Should-Cost Modeling &amp; Cost Estimation Engine. See TDD Section 5.08 / SRS
 * Section 4.8. Roles per SRS 4.8.7: Costing Engineer, Purchasing/Sourcing Manager (mapped
 * to the existing SOURCING_LEAD role - no separate "manager" role was seeded and adding a
 * near-duplicate of SOURCING_LEAD felt worse than reusing it; flagged for confirmation),
 * Product Cost Engineer (new role, seeded this session).
 */
@RestController
@RequestMapping("/api/v1/should-cost")
@RequiredArgsConstructor
@Tag(name = "Should-Cost Modeling & Cost Estimation Engine")
@RequiresModule("PRODUCT_COSTING")
public class ShouldCostController {

    private final ShouldCostService shouldCostService;

    @PostMapping("/generate")
    @PreAuthorize("@permissionAuth.has('SHOULD_COST')")
    @Operation(summary = "Generate a full should-cost build-up for a BOM node (SC-01)")
    public ResponseEntity<Response> generate(@Valid @RequestBody GenerateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shouldCostService.generate(request));
    }

    @PostMapping("/parametric")
    @PreAuthorize("@permissionAuth.has('SHOULD_COST')")
    @Operation(summary = "Rapid parametric should-cost estimate - weight + material family + process family only (SC-02)")
    public ResponseEntity<ParametricResponse> parametric(@Valid @RequestBody ParametricRequest request) {
        return ResponseEntity.ok(shouldCostService.rapidParametric(request));
    }

    @GetMapping("/{buildUpId}/waterfall")
    @PreAuthorize("@permissionAuth.has('SHOULD_COST') or @permissionAuth.has('COSTING_VIEW')")
    @Operation(summary = "Cost breakdown waterfall data (SC-03)")
    public ResponseEntity<WaterfallResponse> waterfall(@PathVariable String buildUpId) {
        return ResponseEntity.ok(shouldCostService.waterfall(buildUpId));
    }

    @PostMapping("/variance")
    @PreAuthorize("@permissionAuth.has('SHOULD_COST') or @permissionAuth.has('COSTING_VIEW')")
    @Operation(summary = "Percentage/absolute variance between should-cost and a quoted price (SC-04)")
    public ResponseEntity<VarianceResponse> variance(@Valid @RequestBody VarianceRequest request) {
        return ResponseEntity.ok(shouldCostService.varianceAgainstQuote(request));
    }

    @PostMapping("/compare")
    @PreAuthorize("@permissionAuth.has('SHOULD_COST') or @permissionAuth.has('COSTING_VIEW')")
    @Operation(summary = "Compare should-cost across two or more regional/scenario variants (SC-05)")
    public ResponseEntity<RegionalCompareResponse> compare(@Valid @RequestBody RegionalCompareRequest request) {
        return ResponseEntity.ok(shouldCostService.regionalCompare(request));
    }

    @GetMapping("/by-bom-node/{bomNodeId}/scenarios")
    @PreAuthorize("@permissionAuth.has('SHOULD_COST') or @permissionAuth.has('COSTING_VIEW')")
    @Operation(summary = "Every saved should-cost scenario for a BOM node (SC-06)")
    public ResponseEntity<List<Response>> scenarios(@PathVariable String bomNodeId) {
        return ResponseEntity.ok(shouldCostService.listScenarios(bomNodeId));
    }

    @PutMapping("/{buildUpId}/lock")
    @PreAuthorize("@permissionAuth.has('SHOULD_COST')")
    @Operation(summary = "Lock a should-cost estimate (read-only) once linked to a formal negotiation record")
    public ResponseEntity<Response> lock(@PathVariable String buildUpId) {
        return ResponseEntity.ok(shouldCostService.lock(buildUpId));
    }

    @GetMapping("/components")
    @PreAuthorize("@permissionAuth.has('SHOULD_COST') or @permissionAuth.has('COSTING_VIEW')")
    @Operation(summary = "List active cost component definitions (P2-01)")
    public ResponseEntity<List<CostComponentDefinitionDto>> listComponents(@RequestParam(required = false) String tenantId) {
        return ResponseEntity.ok(shouldCostService.listActiveComponents(tenantId));
    }
    @PostMapping("/deep-variance")
    @PreAuthorize("@permissionAuth.has('SHOULD_COST') or @permissionAuth.has('COSTING_VIEW')")
    @Operation(summary = "Component-level variance (P2-01)")
    public ResponseEntity<DeepVarianceResponse> deepVariance(@Valid @RequestBody DeepVarianceRequest request) {
        return ResponseEntity.ok(shouldCostService.deepVariance(request));
    }

}
