package com.automotive.costing.purchase.controller;

import com.automotive.costing.platform.access.RequiresModule;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.CreateFromPurchaseRequestRequest;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.ClosePoRequest;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.CreatePurchaseOrderRequest;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.DecidePoRequest;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.IssuePoRequest;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.PurchaseOrderResponse;
import com.automotive.costing.purchase.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Wave D — Purchase Order APIs. Module PURCHASE only.
 * Does not post to SAP MM. Human approval mandatory.
 */
@RestController
@RequiredArgsConstructor
@RequiresModule("PURCHASE")
@Tag(name = "Purchase Orders")
public class PurchaseOrderController {

    private final PurchaseOrderService service;

    @PostMapping("/api/v1/purchase-orders")
    @Operation(summary = "Create PO in DRAFT (optional link to PR / RFQ)")
    public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody CreatePurchaseOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PostMapping("/api/v1/purchase-requests/{prId}/create-purchase-order")
    @Operation(summary = "Create DRAFT PO from APPROVED/RFQ_LINKED/RELEASED PR lines")
    public ResponseEntity<PurchaseOrderResponse> createFromPurchaseRequest(
            @PathVariable String prId,
            @Valid @RequestBody CreateFromPurchaseRequestRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createFromPurchaseRequest(prId, request));
    }

    @GetMapping("/api/v1/purchase-orders")
    @Operation(summary = "List POs for current tenant")
    public ResponseEntity<List<PurchaseOrderResponse>> list(
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(service.list(status));
    }

    @GetMapping("/api/v1/purchase-orders/{id}")
    @Operation(summary = "Get PO by id (tenant-scoped)")
    public ResponseEntity<PurchaseOrderResponse> get(@PathVariable String id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping("/api/v1/purchase-orders/{id}/submit")
    @Operation(summary = "DRAFT/REWORK → SUBMITTED → APPROVAL")
    public ResponseEntity<PurchaseOrderResponse> submit(@PathVariable String id) {
        return ResponseEntity.ok(service.submit(id));
    }

    @PostMapping("/api/v1/purchase-orders/{id}/decide")
    @Operation(summary = "APPROVAL → APPROVED or REJECTED (human only)")
    public ResponseEntity<PurchaseOrderResponse> decide(
            @PathVariable String id,
            @Valid @RequestBody DecidePoRequest request
    ) {
        return ResponseEntity.ok(service.decide(id, request));
    }

    @PostMapping("/api/v1/purchase-orders/{id}/rework")
    @Operation(summary = "REJECTED → REWORK")
    public ResponseEntity<PurchaseOrderResponse> rework(@PathVariable String id) {
        return ResponseEntity.ok(service.rework(id));
    }

    @PostMapping("/api/v1/purchase-orders/{id}/issue")
    @Operation(summary = "APPROVED → ISSUED (blocks NOT_AVAILABLE prices)")
    public ResponseEntity<PurchaseOrderResponse> issue(
            @PathVariable String id,
            @RequestBody(required = false) IssuePoRequest request
    ) {
        return ResponseEntity.ok(service.issue(id, request == null ? new IssuePoRequest(null) : request));
    }

    @PostMapping("/api/v1/purchase-orders/{id}/cancel")
    @Operation(summary = "Cancel PO from non-terminal states")
    public ResponseEntity<PurchaseOrderResponse> cancel(
            @PathVariable String id,
            @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(service.cancel(id, reason));
    }

    @PostMapping("/api/v1/purchase-orders/{id}/close")
    @Operation(summary = "ISSUED → PARTIALLY_RECEIVED or CLOSED (light operational; not ERP GR/IR)")
    public ResponseEntity<PurchaseOrderResponse> close(
            @PathVariable String id,
            @RequestBody(required = false) ClosePoRequest request
    ) {
        return ResponseEntity.ok(service.close(id, request == null ? new ClosePoRequest(null, false) : request));
    }
}
