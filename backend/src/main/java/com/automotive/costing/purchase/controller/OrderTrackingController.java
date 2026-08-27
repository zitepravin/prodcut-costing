package com.automotive.costing.purchase.controller;

import com.automotive.costing.platform.access.RequiresModule;
import com.automotive.costing.purchase.dto.OrderTrackingDtos.HumanCorrectionRequest;
import com.automotive.costing.purchase.dto.OrderTrackingDtos.InboundTrackingRequest;
import com.automotive.costing.purchase.dto.OrderTrackingDtos.TrackingResponse;
import com.automotive.costing.purchase.service.OrderTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * P3-05 Order tracking. CPI posts inbound; UI is read-only except human correction.
 * Does not post to SAP MM.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Order Tracking (P3-05)")
@RequiresModule("PURCHASE")
public class OrderTrackingController {

    private final OrderTrackingService service;

    @PostMapping("/api/v1/erp/cpi/purchase-tracking")
    @Operation(summary = "CPI inbound: land ASN / qty shipped for a RELEASED purchase request (no SAP write)")
    public ResponseEntity<TrackingResponse> ingest(@Valid @RequestBody InboundTrackingRequest request) {
        return ResponseEntity.ok(service.ingestFromCpi(request));
    }

    @GetMapping("/api/v1/purchase-requests/{id}/tracking")
    @Operation(summary = "List order tracking rows for a purchase request")
    public ResponseEntity<List<TrackingResponse>> list(@PathVariable String id) {
        return ResponseEntity.ok(service.listForPurchaseRequest(id));
    }

    @PostMapping("/api/v1/purchase-requests/{id}/tracking/{trackingId}/correct")
    @Operation(summary = "Human correction of tracking fields (reason required; no SAP write)")
    public ResponseEntity<TrackingResponse> correct(
            @PathVariable String id,
            @PathVariable String trackingId,
            @Valid @RequestBody HumanCorrectionRequest request
    ) {
        return ResponseEntity.ok(service.humanCorrect(id, trackingId, request));
    }
}
