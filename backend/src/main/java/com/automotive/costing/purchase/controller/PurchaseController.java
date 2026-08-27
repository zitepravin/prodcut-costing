package com.automotive.costing.purchase.controller;

import com.automotive.costing.platform.access.RequiresModule;
import com.automotive.costing.purchase.dto.PurchaseDtos.CreateFromMvbRequest;
import com.automotive.costing.purchase.dto.PurchaseDtos.CreateStandalonePurchaseRequest;
import com.automotive.costing.purchase.dto.PurchaseDtos.DecideRequest;
import com.automotive.costing.purchase.dto.PurchaseDtos.PurchaseExportPackage;
import com.automotive.costing.purchase.dto.PurchaseDtos.PurchaseRequestResponse;
import com.automotive.costing.purchase.dto.PurchaseDtos.ReleaseRequest;
import com.automotive.costing.purchase.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Purchase Request (P3-01)")
@RequiresModule("PURCHASE")
public class PurchaseController {

    private final PurchaseService service;

    @PostMapping("/api/v1/bom-nodes/{bomNodeId}/purchase-requests")
    @Operation(summary = "Create DRAFT purchase request from DECIDED Make-vs-Buy or manual override")
    public ResponseEntity<PurchaseRequestResponse> create(
            @PathVariable String bomNodeId,
            @Valid @RequestBody CreateFromMvbRequest request
    ) {
        return ResponseEntity.ok(service.create(bomNodeId, request));
    }

    @GetMapping("/api/v1/bom-nodes/{bomNodeId}/purchase-requests")
    @Operation(summary = "List purchase requests for BOM node")
    public ResponseEntity<List<PurchaseRequestResponse>> listByBom(@PathVariable String bomNodeId) {
        return ResponseEntity.ok(service.listByBomNode(bomNodeId));
    }

    @GetMapping("/api/v1/programs/{programId}/purchase-requests")
    @Operation(summary = "List purchase requests for program")
    public ResponseEntity<List<PurchaseRequestResponse>> listByProgram(@PathVariable String programId) {
        return ResponseEntity.ok(service.listByProgram(programId));
    }

    @GetMapping("/api/v1/purchase-requests/{id}")
    @Operation(summary = "Get purchase request")
    public ResponseEntity<PurchaseRequestResponse> get(@PathVariable String id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping("/api/v1/purchase-requests/{id}/submit")
    @Operation(summary = "DRAFT → SUBMITTED")
    public ResponseEntity<PurchaseRequestResponse> submit(@PathVariable String id) {
        return ResponseEntity.ok(service.submit(id));
    }

    @PostMapping("/api/v1/purchase-requests/{id}/decide")
    @Operation(summary = "Human APPROVE or REJECT (reason required)")
    public ResponseEntity<PurchaseRequestResponse> decide(
            @PathVariable String id,
            @Valid @RequestBody DecideRequest request
    ) {
        return ResponseEntity.ok(service.decide(id, request));
    }

    @PostMapping("/api/v1/purchase-requests/{id}/release")
    @Operation(summary = "APPROVED → RELEASED (blocks if any line NOT_AVAILABLE)")
    public ResponseEntity<PurchaseRequestResponse> release(
            @PathVariable String id,
            @RequestBody(required = false) ReleaseRequest request
    ) {
        return ResponseEntity.ok(service.release(id, request == null ? new ReleaseRequest(null) : request));
    }

    @GetMapping("/api/v1/purchase-requests/{id}/export-package")
    @Operation(summary = "Read-only JSON for CPI/ERP — does not post to SAP MM")
    public ResponseEntity<PurchaseExportPackage> exportPackage(@PathVariable String id) {
        return ResponseEntity.ok(service.exportPackage(id));
    }

    @PostMapping("/api/v1/purchase-requests/{id}/start-review")
    @Operation(summary = "SUBMITTED → UNDER_REVIEW")
    public ResponseEntity<PurchaseRequestResponse> startReview(@PathVariable String id) {
        return ResponseEntity.ok(service.startReview(id));
    }

    @PostMapping("/api/v1/purchase-requests/{id}/cancel")
    @Operation(summary = "Cancel PR (allowed from DRAFT/SUBMITTED/UNDER_REVIEW/APPROVED)")
    public ResponseEntity<PurchaseRequestResponse> cancel(
            @PathVariable String id,
            @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(service.cancel(id, reason));
    }

    @PostMapping("/api/v1/purchase-requests/{id}/reopen")
    @Operation(summary = "REJECTED → DRAFT for rework")
    public ResponseEntity<PurchaseRequestResponse> reopen(@PathVariable String id) {
        return ResponseEntity.ok(service.reopenFromRejected(id));
    }


    @PostMapping("/api/v1/purchase-requests/{id}/create-rfq")
    @Operation(summary = "Wave C: APPROVED PR → create RFQ via existing RFQ service (no second RFQ)")
    public ResponseEntity<PurchaseRequestResponse> createRfq(@PathVariable String id) {
        return ResponseEntity.ok(service.createRfqFromApproved(id));
    }

}
