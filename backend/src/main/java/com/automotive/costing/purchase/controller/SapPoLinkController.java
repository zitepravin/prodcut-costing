package com.automotive.costing.purchase.controller;

import com.automotive.costing.platform.access.RequiresModule;
import com.automotive.costing.purchase.dto.SapPoLinkDtos.HumanPoLinkRequest;
import com.automotive.costing.purchase.dto.SapPoLinkDtos.SapPoLinkRequest;
import com.automotive.costing.purchase.dto.SapPoLinkDtos.SapPoLinkResponse;
import com.automotive.costing.purchase.service.SapPoLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * P3-09a CPI / human SAP PO link. Inbound confirmation only — does not post to SAP MM.
 * Auth: application JWT (service account or admin user). Gateway mTLS is infrastructure-side.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "SAP PO Link (P3-09)")
@RequiresModule("PURCHASE")
public class SapPoLinkController {

    private final SapPoLinkService service;

    @PostMapping("/api/v1/erp/cpi/purchase-requests/{id}/sap-po-link")
    @Operation(summary = "CPI callback: store external SAP PO number for RELEASED PR (no SAP write)")
    public ResponseEntity<SapPoLinkResponse> cpiLink(
            @PathVariable String id,
            @Valid @RequestBody SapPoLinkRequest request
    ) {
        return ResponseEntity.ok(service.linkFromCpi(id, request));
    }

    @PostMapping("/api/v1/purchase-requests/{id}/sap-po-link")
    @Operation(summary = "Human admin set/overwrite SAP PO number with reason (RELEASED only; no SAP write)")
    public ResponseEntity<SapPoLinkResponse> humanLink(
            @PathVariable String id,
            @Valid @RequestBody HumanPoLinkRequest request
    ) {
        return ResponseEntity.ok(service.linkFromHuman(id, request));
    }
}
