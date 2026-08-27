package com.automotive.costing.dashboard.controller;

import com.automotive.costing.dashboard.dto.DashboardDtos.ProgramCostPositionResponse;
import com.automotive.costing.dashboard.service.ProgramCostPositionService;
import com.automotive.costing.platform.access.RequiresModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/programs")
@RequiredArgsConstructor
@Tag(name = "Program Cost Position")
@RequiresModule("PRODUCT_COSTING")
public class ProgramCostPositionController {

    private final ProgramCostPositionService programCostPositionService;

    @GetMapping("/{programId}/cost-position")
    @PreAuthorize("@permissionAuth.has('COSTING_VIEW') or @permissionAuth.has('SHOULD_COST')")
    @Operation(summary = "Program should-cost / target position (one build-up per BOM node; no scenario double-count)")
    public ResponseEntity<ProgramCostPositionResponse> costPosition(
            @PathVariable String programId,
            @RequestParam(required = false, defaultValue = "APPROVED_THEN_CURRENT_THEN_LATEST") String scenarioPolicy,
            @RequestParam(required = false, defaultValue = "true") boolean includeDrivers
    ) {
        return ResponseEntity.ok(programCostPositionService.position(programId, scenarioPolicy, includeDrivers));
    }
}
