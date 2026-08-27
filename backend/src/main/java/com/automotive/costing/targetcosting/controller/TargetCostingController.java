package com.automotive.costing.targetcosting.controller;

import com.automotive.costing.platform.access.RequiresModule;

import com.automotive.costing.targetcosting.dto.TargetCostingDtos.*;
import com.automotive.costing.targetcosting.service.TargetCostingService;
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
@RequestMapping("/api/v1/target-costing")
@RequiredArgsConstructor
@Tag(name = "Target Costing")
@RequiresModule("PRODUCT_COSTING")
public class TargetCostingController {

    private final TargetCostingService targetCostingService;

    @PostMapping("/targets")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'ADMIN')")
    @Operation(summary = "Create a part-level target cost (DRAFT)")
    public ResponseEntity<TargetResponse> create(@Valid @RequestBody CreateTargetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(targetCostingService.create(request));
    }

    @PutMapping("/targets/{targetCostId}")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'ADMIN')")
    @Operation(summary = "Update a target cost (not while SUBMITTED)")
    public ResponseEntity<TargetResponse> update(
            @PathVariable String targetCostId,
            @Valid @RequestBody UpdateTargetRequest request) {
        return ResponseEntity.ok(targetCostingService.update(targetCostId, request));
    }

    @PostMapping("/targets/{targetCostId}/activate")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'ADMIN')")
    @Operation(summary = "Activate target; supersedes prior ACTIVE on same BOM node")
    public ResponseEntity<TargetResponse> activate(@PathVariable String targetCostId) {
        return ResponseEntity.ok(targetCostingService.activate(targetCostId));
    }

    @PostMapping("/targets/{targetCostId}/mark-submitted")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'ADMIN')")
    @Operation(summary = "Mark approvalStatus=SUBMITTED after workflow submit (entityType=TargetCostRecord)")
    public ResponseEntity<TargetResponse> markSubmitted(@PathVariable String targetCostId) {
        return ResponseEntity.ok(targetCostingService.markSubmitted(targetCostId));
    }

    @GetMapping("/targets/{targetCostId}")
    @Operation(summary = "Get target cost by id")
    public ResponseEntity<TargetResponse> get(@PathVariable String targetCostId) {
        return ResponseEntity.ok(targetCostingService.getTarget(targetCostId));
    }

    @GetMapping("/targets")
    @Operation(summary = "List targets by bomNodeId or programId")
    public ResponseEntity<List<TargetResponse>> list(
            @RequestParam(required = false) String bomNodeId,
            @RequestParam(required = false) String programId) {
        if (bomNodeId != null && !bomNodeId.isBlank()) {
            return ResponseEntity.ok(targetCostingService.listByBomNode(bomNodeId));
        }
        if (programId != null && !programId.isBlank()) {
            return ResponseEntity.ok(targetCostingService.listByProgram(programId));
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/variance")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'PURCHASING_BUYER', 'ADMIN')")
    @Operation(summary = "Compute and persist target vs should-cost or BOM roll-up variance")
    public ResponseEntity<VarianceResponse> computeVariance(@Valid @RequestBody ComputeVarianceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(targetCostingService.computeVariance(request));
    }

    @GetMapping("/variance")
    @Operation(summary = "Variance history for a BOM node")
    public ResponseEntity<List<VarianceResponse>> varianceHistory(@RequestParam String bomNodeId) {
        return ResponseEntity.ok(targetCostingService.varianceHistory(bomNodeId));
    }

    @GetMapping("/variance/exceeding")
    @Operation(summary = "Variances that exceed configured threshold (KPI feed)")
    public ResponseEntity<ExceedingVarianceSummary> exceeding() {
        return ResponseEntity.ok(targetCostingService.listExceedingThreshold());
    }
}
