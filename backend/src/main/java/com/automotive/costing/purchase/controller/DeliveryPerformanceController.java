package com.automotive.costing.purchase.controller;

import com.automotive.costing.platform.access.RequiresModule;
import com.automotive.costing.purchase.dto.DeliveryPerformanceDtos.OnTimeOverrideRequest;
import com.automotive.costing.purchase.dto.DeliveryPerformanceDtos.PerformanceSummary;
import com.automotive.costing.purchase.dto.DeliveryPerformanceDtos.ShipmentPerformance;
import com.automotive.costing.purchase.service.DeliveryPerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Delivery Performance (P3-06)")
@RequiresModule("PURCHASE")
public class DeliveryPerformanceController {

    private final DeliveryPerformanceService service;

    @GetMapping("/api/v1/purchase-requests/{id}/delivery-performance")
    @Operation(summary = "Computed delivery performance from tracking + PR lines (missing → NOT_AVAILABLE)")
    public ResponseEntity<PerformanceSummary> summarize(@PathVariable String id) {
        return ResponseEntity.ok(service.summarize(id));
    }

    @PostMapping("/api/v1/purchase-requests/{id}/tracking/{trackingId}/on-time-override")
    @Operation(summary = "Human override of on-time classification (reason required; no SAP write)")
    public ResponseEntity<ShipmentPerformance> overrideOnTime(
            @PathVariable String id,
            @PathVariable String trackingId,
            @Valid @RequestBody OnTimeOverrideRequest request
    ) {
        return ResponseEntity.ok(service.overrideOnTime(id, trackingId, request));
    }
}
