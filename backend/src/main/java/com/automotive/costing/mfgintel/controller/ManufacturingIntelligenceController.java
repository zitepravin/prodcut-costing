package com.automotive.costing.mfgintel.controller;

import com.automotive.costing.mfgintel.dto.MfgIntelDtos.*;
import com.automotive.costing.mfgintel.service.ManufacturingIntelligenceService;
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
@RequestMapping("/api/v1/mfg-intel")
@RequiredArgsConstructor
@Tag(name = "Manufacturing Intelligence (D1/D2)")
@RequiresModule("PRODUCT_COSTING")
public class ManufacturingIntelligenceController {

    private final ManufacturingIntelligenceService service;

    @GetMapping("/activities")
    public ResponseEntity<List<ActivityTypeResponse>> listActivities() {
        return ResponseEntity.ok(service.listActivities());
    }

    @PostMapping("/activities")
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'MASTER_DATA_ADMIN', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<ActivityTypeResponse> createActivity(@Valid @RequestBody CreateActivityTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createActivity(request));
    }

    @PostMapping("/activities/calculate")
    @Operation(summary = "D1 activity cost = quantity × rate")
    public ResponseEntity<ActivityCostResponse> calculateActivity(@Valid @RequestBody ActivityCostRequest request) {
        return ResponseEntity.ok(service.calculateActivityCost(request));
    }

    @GetMapping("/capacity-centers")
    public ResponseEntity<List<CapacityCenterResponse>> listCenters(
            @RequestParam(required = false) String periodCode) {
        return ResponseEntity.ok(service.listCenters(periodCode));
    }

    @PostMapping("/capacity-centers")
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'MASTER_DATA_ADMIN', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<CapacityCenterResponse> createCenter(@Valid @RequestBody CreateCapacityCenterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createCenter(request));
    }

    @PostMapping("/capacity-centers/{id}/loads")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<LoadResponse> addLoad(
            @PathVariable String id,
            @Valid @RequestBody AddLoadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addLoad(id, request));
    }

    @GetMapping("/capacity-centers/{id}/utilization")
    @Operation(summary = "D2 capacity utilization / residual / overload status")
    public ResponseEntity<UtilizationResponse> utilization(@PathVariable String id) {
        return ResponseEntity.ok(service.utilization(id));
    }
}
