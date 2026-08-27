package com.automotive.costing.rfq.controller;

import com.automotive.costing.rfq.dto.PortalDtos.AcceptInviteRequest;
import com.automotive.costing.rfq.dto.PortalDtos.CreateInviteRequest;
import com.automotive.costing.rfq.dto.PortalDtos.InviteResponse;
import com.automotive.costing.rfq.dto.PortalDtos.PortalLoginRequest;
import com.automotive.costing.rfq.dto.PortalDtos.PortalQuoteSubmitRequest;
import com.automotive.costing.rfq.dto.PortalDtos.GrantRfqAccessRequest;
import com.automotive.costing.rfq.dto.PortalDtos.CreateClarificationRequest;
import com.automotive.costing.rfq.dto.PortalDtos.ClarificationResponse;
import com.automotive.costing.rfq.dto.PortalDtos.RespondClarificationRequest;
import com.automotive.costing.rfq.dto.PortalDtos.CreateAttachmentRequest;
import com.automotive.costing.rfq.dto.PortalDtos.AttachmentResponse;
import com.automotive.costing.rfq.dto.PortalDtos.PortalRfqAccessResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import com.automotive.costing.rfq.dto.PortalDtos.PortalTokenResponse;
import com.automotive.costing.rfq.dto.RfqDtos.QuoteResponse;
import com.automotive.costing.platform.access.RequiresModule;
import com.automotive.costing.rfq.service.SupplierPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portal")
@RequiredArgsConstructor
@Tag(name = "Supplier Portal (Phase 2)")
public class SupplierPortalController {

    private final SupplierPortalService portalService;

    @PostMapping("/invites")
    @RequiresModule("PRODUCT_COSTING")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Invite supplier contact to portal (returns one-time invite token)")
    public ResponseEntity<InviteResponse> invite(@Valid @RequestBody CreateInviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.createInvite(request));
    }

    @PostMapping("/accept-invite")
    @Operation(summary = "Accept invite and set password — public")
    public ResponseEntity<PortalTokenResponse> accept(@Valid @RequestBody AcceptInviteRequest request) {
        return ResponseEntity.ok(portalService.acceptInvite(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Supplier portal login — public")
    public ResponseEntity<PortalTokenResponse> login(@Valid @RequestBody PortalLoginRequest request) {
        return ResponseEntity.ok(portalService.login(request));
    }

    @PostMapping("/quotes")
    @Operation(summary = "Submit quote as authenticated portal supplier (supplierId from token)")
    public ResponseEntity<QuoteResponse> submitQuote(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody PortalQuoteSubmitRequest request) {
        String supplierId = portalService.requireSupplierIdFromPortalToken(authorization);
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.submitQuote(supplierId, request));
    }

    @PostMapping("/rfq-access")
    @RequiresModule("PRODUCT_COSTING")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Grant supplier portal access to an RFQ")
    public ResponseEntity<PortalRfqAccessResponse> grantAccess(@Valid @RequestBody GrantRfqAccessRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.grantRfqAccess(request));
    }

    @PostMapping("/rfq-access/{accessId}/revoke")
    @RequiresModule("PRODUCT_COSTING")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Revoke supplier portal RFQ access")
    public ResponseEntity<PortalRfqAccessResponse> revoke(@PathVariable String accessId) {
        return ResponseEntity.ok(portalService.revokeRfqAccess(accessId));
    }

    @GetMapping("/my-rfqs")
    @Operation(summary = "List RFQs accessible to the portal supplier (Bearer portal JWT)")
    public ResponseEntity<List<PortalRfqAccessResponse>> myRfqs(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String supplierId = portalService.requireSupplierIdFromPortalToken(authorization);
        return ResponseEntity.ok(portalService.listActiveAccessForSupplier(supplierId));
    }

    @PostMapping("/clarifications")
    @Operation(summary = "Seller: request clarification on a granted RFQ")
    public ResponseEntity<ClarificationResponse> createClarification(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateClarificationRequest request) {
        String supplierId = portalService.requireSupplierIdFromPortalToken(authorization);
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.createClarification(supplierId, request));
    }

    @GetMapping("/clarifications")
    @Operation(summary = "Seller: list own clarifications for RFQ")
    public ResponseEntity<List<ClarificationResponse>> listMyClarifications(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @org.springframework.web.bind.annotation.RequestParam String rfqId) {
        String supplierId = portalService.requireSupplierIdFromPortalToken(authorization);
        return ResponseEntity.ok(portalService.listClarificationsForSupplier(supplierId, rfqId));
    }

    @GetMapping("/rfqs/{rfqId}/attachments")
    @Operation(summary = "Seller: list RFQ attachment metadata (granted RFQ only)")
    public ResponseEntity<List<AttachmentResponse>> listAttachmentsPortal(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String rfqId) {
        String supplierId = portalService.requireSupplierIdFromPortalToken(authorization);
        return ResponseEntity.ok(portalService.listAttachmentsForPortal(supplierId, rfqId));
    }

    @GetMapping("/buyer/rfqs/{rfqId}/clarifications")
    @RequiresModule("PRODUCT_COSTING")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Buyer: list clarifications on RFQ")
    public ResponseEntity<List<ClarificationResponse>> listBuyerClarifications(@PathVariable String rfqId) {
        return ResponseEntity.ok(portalService.listClarificationsForBuyer(rfqId));
    }

    @PostMapping("/buyer/clarifications/{id}/respond")
    @RequiresModule("PRODUCT_COSTING")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Buyer: answer seller clarification")
    public ResponseEntity<ClarificationResponse> respond(
            @PathVariable String id,
            @Valid @RequestBody RespondClarificationRequest request) {
        return ResponseEntity.ok(portalService.respondClarification(id, request));
    }

    @PostMapping("/buyer/attachments")
    @RequiresModule("PRODUCT_COSTING")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Buyer: register RFQ attachment metadata (URI to file store)")
    public ResponseEntity<AttachmentResponse> addAttachment(@Valid @RequestBody CreateAttachmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.addAttachment(request));
    }

    @GetMapping("/buyer/rfqs/{rfqId}/attachments")
    @RequiresModule("PRODUCT_COSTING")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Buyer: list RFQ attachments")
    public ResponseEntity<List<AttachmentResponse>> listBuyerAttachments(@PathVariable String rfqId) {
        return ResponseEntity.ok(portalService.listAttachments(rfqId));
    }

}
