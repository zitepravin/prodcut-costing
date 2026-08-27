package com.automotive.costing.rfq.controller;

import com.automotive.costing.rfq.dto.RfqDtos.AddRfqLineRequest;
import com.automotive.costing.rfq.dto.RfqDtos.CreateRfqRequest;
import com.automotive.costing.rfq.dto.RfqDtos.CreateSupplierRequest;
import com.automotive.costing.rfq.dto.RfqDtos.QuoteResponse;
import com.automotive.costing.rfq.dto.RfqDtos.RfqDetailResponse;
import com.automotive.costing.rfq.dto.RfqDtos.RfqLineResponse;
import com.automotive.costing.rfq.dto.RfqDtos.RfqResponse;
import com.automotive.costing.rfq.dto.RfqDtos.SubmitQuoteRequest;
import com.automotive.costing.rfq.dto.RfqDtos.SupplierResponse;
import com.automotive.costing.rfq.dto.RfqDtos.UpdateQuoteStatusRequest;
import com.automotive.costing.rfq.entity.SupplierStatus;
import com.automotive.costing.rfq.service.RfqService;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.rfq.service.SupplierQuoteService;
import com.automotive.costing.rfq.service.QuoteShouldCostComparisonService;
import com.automotive.costing.rfq.dto.RfqDtos.QuoteShouldCostComparisonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.automotive.costing.platform.access.RequiresModule;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rfq")
@RequiredArgsConstructor
@Tag(name = "RFQ & Supplier Costing (M09/M10)")
@RequiresModule("PRODUCT_COSTING")
public class RfqSupplierController {

    private final SupplierMasterService supplierService;
    private final RfqService rfqService;
    private final QuoteShouldCostComparisonService quoteShouldCostComparisonService;
    private final SupplierQuoteService quoteService;

    // --- Suppliers ---

    @PostMapping("/suppliers")
    @PreAuthorize("@permissionAuth.has('RFQ_CREATE') or @permissionAuth.has('SUPPLIER_CREATE')")
    @Operation(summary = "Create supplier master")
    public ResponseEntity<SupplierResponse> createSupplier(@Valid @RequestBody CreateSupplierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(request));
    }

    @GetMapping("/suppliers")
    @PreAuthorize("@permissionAuth.has('RFQ_VIEW')")
    @Operation(summary = "List active suppliers")
    public ResponseEntity<List<SupplierResponse>> listSuppliers() {
        return ResponseEntity.ok(supplierService.listActive());
    }

    @GetMapping("/suppliers/{supplierId}")
    @PreAuthorize("@permissionAuth.has('RFQ_VIEW')")
    @Operation(summary = "Get supplier by id")
    public ResponseEntity<SupplierResponse> getSupplier(@PathVariable String supplierId) {
        return ResponseEntity.ok(supplierService.get(supplierId));
    }

    @PostMapping("/suppliers/{supplierId}/status")
    @PreAuthorize("@permissionAuth.has('RFQ_EDIT')")
    @Operation(summary = "Set supplier status (ACTIVE|INACTIVE|BLOCKED)")
    public ResponseEntity<SupplierResponse> setSupplierStatus(
            @PathVariable String supplierId,
            @RequestParam SupplierStatus status) {
        return ResponseEntity.ok(supplierService.setStatus(supplierId, status));
    }

    // --- RFQ events ---

    @PostMapping("/events")
    @PreAuthorize("@permissionAuth.has('RFQ_CREATE')")
    @Operation(summary = "Create RFQ event (DRAFT)")
    public ResponseEntity<RfqResponse> createRfq(@Valid @RequestBody CreateRfqRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rfqService.create(request));
    }

    @GetMapping("/events")
    @PreAuthorize("@permissionAuth.has('RFQ_VIEW')")
    @Operation(summary = "List RFQs for a vehicle program")
    public ResponseEntity<List<RfqResponse>> listByProgram(@RequestParam String programId) {
        return ResponseEntity.ok(rfqService.listByProgram(programId));
    }

    @GetMapping("/events/{rfqId}")
    @PreAuthorize("@permissionAuth.has('RFQ_VIEW')")
    @Operation(summary = "RFQ detail with lines")
    public ResponseEntity<RfqDetailResponse> getRfq(@PathVariable String rfqId) {
        return ResponseEntity.ok(rfqService.getDetail(rfqId));
    }

    @PostMapping("/events/{rfqId}/open")
    @PreAuthorize("@permissionAuth.has('RFQ_EDIT')")
    @Operation(summary = "Open RFQ for quote intake")
    public ResponseEntity<RfqResponse> open(@PathVariable String rfqId) {
        return ResponseEntity.ok(rfqService.open(rfqId));
    }

    @PostMapping("/events/{rfqId}/close")
    @PreAuthorize("@permissionAuth.has('RFQ_EDIT')")
    @Operation(summary = "Close RFQ")
    public ResponseEntity<RfqResponse> close(@PathVariable String rfqId) {
        return ResponseEntity.ok(rfqService.close(rfqId));
    }

    @PostMapping("/events/{rfqId}/cancel")
    @PreAuthorize("@permissionAuth.has('RFQ_EDIT')")
    @Operation(summary = "Cancel RFQ")
    public ResponseEntity<RfqResponse> cancel(@PathVariable String rfqId) {
        return ResponseEntity.ok(rfqService.cancel(rfqId));
    }

    @PostMapping("/events/{rfqId}/lines")
    @PreAuthorize("@permissionAuth.has('RFQ_EDIT')")
    @Operation(summary = "Add BOM node line to RFQ")
    public ResponseEntity<RfqLineResponse> addLine(
            @PathVariable String rfqId,
            @Valid @RequestBody AddRfqLineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rfqService.addLine(rfqId, request));
    }

    // --- Quotes ---

    @PostMapping("/events/{rfqId}/quotes")
    @PreAuthorize("@permissionAuth.has('RFQ_EDIT')")
    @Operation(summary = "Submit supplier quote against an RFQ line")
    public ResponseEntity<QuoteResponse> submitQuote(
            @PathVariable String rfqId,
            @Valid @RequestBody SubmitQuoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quoteService.submit(rfqId, request));
    }

    @GetMapping("/events/{rfqId}/quotes")
    @PreAuthorize("@permissionAuth.has('RFQ_VIEW')")
    @Operation(summary = "List quotes for RFQ (SEC-04 wall enforced when configured)")
    public ResponseEntity<List<QuoteResponse>> listQuotes(@PathVariable String rfqId) {
        return ResponseEntity.ok(quoteService.listByRfq(rfqId));
    }

    @PostMapping("/quotes/{quoteId}/status")
    @PreAuthorize("@permissionAuth.has('RFQ_EDIT')")
    @Operation(summary = "Update quote status (ACCEPTED supersedes prior accepted for same BOM node)")
    public ResponseEntity<QuoteResponse> updateQuoteStatus(
            @PathVariable String quoteId,
            @Valid @RequestBody UpdateQuoteStatusRequest request) {
        return ResponseEntity.ok(quoteService.updateStatus(quoteId, request));
    }

    @GetMapping("/quotes/accepted")
    @PreAuthorize("@permissionAuth.has('RFQ_VIEW')")
    @Operation(summary = "Accepted quotes for a BOM node (M10 cost source)")
    public ResponseEntity<List<QuoteResponse>> acceptedForBomNode(@RequestParam String bomNodeId) {
        return ResponseEntity.ok(quoteService.listByBomNode(bomNodeId));
    }

    @GetMapping("/events/{rfqId}/quote-comparison")
    @PreAuthorize("@permissionAuth.has('RFQ_VIEW')")
    @Operation(summary = "Buyer analysis: quotes vs latest should-cost per BOM node (no invented FX)")
    public ResponseEntity<QuoteShouldCostComparisonResponse> quoteComparison(@PathVariable String rfqId) {
        return ResponseEntity.ok(quoteShouldCostComparisonService.compare(rfqId));
    }

}
