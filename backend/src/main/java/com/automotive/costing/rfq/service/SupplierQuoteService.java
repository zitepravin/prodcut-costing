package com.automotive.costing.rfq.service;

import com.automotive.costing.versioning.entity.ChangeTrigger;
import com.automotive.costing.versioning.service.CostVersionService;
import java.util.Map;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.user.entity.ConfidentialityWall;
import com.automotive.costing.platform.user.repository.ConfidentialityWallRepository;
import com.automotive.costing.rfq.dto.RfqDtos.QuoteResponse;
import com.automotive.costing.rfq.dto.RfqDtos.SubmitQuoteRequest;
import com.automotive.costing.rfq.dto.RfqDtos.UpdateQuoteStatusRequest;
import com.automotive.costing.rfq.entity.QuoteStatus;
import com.automotive.costing.rfq.entity.RfqEvent;
import com.automotive.costing.rfq.entity.RfqLine;
import com.automotive.costing.rfq.entity.RfqStatus;
import com.automotive.costing.rfq.entity.SupplierMaster;
import com.automotive.costing.rfq.entity.SupplierQuote;
import com.automotive.costing.rfq.entity.SupplierStatus;
import com.automotive.costing.rfq.repository.RfqLineRepository;
import com.automotive.costing.rfq.repository.SupplierQuoteRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.event.RfqLifecycleEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * M10 Supplier Costing + M09 quote intake.
 * SEC-04: when confidentiality walls exist for the RFQ event, only allowed roles/users see quotes.
 */
@Service
@RequiredArgsConstructor
public class SupplierQuoteService {

    private static final String ENTITY = "SupplierQuote";

    private final SupplierQuoteRepository quoteRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final CostVersionService costVersionService;
    private final RfqLineRepository lineRepository;
    private final RfqService rfqService;
    private final SupplierMasterService supplierService;
    private final ConfidentialityWallRepository wallRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public QuoteResponse submit(String rfqId, SubmitQuoteRequest request) {
        RfqEvent rfq = rfqService.require(rfqId);
        if (rfq.getStatus() != RfqStatus.OPEN) {
            throw new BusinessRuleViolationException("Quotes may only be submitted while RFQ is OPEN", "M09-M10");
        }
        RfqLine line = lineRepository.findByRfqLineId(request.rfqLineId())
                .orElseThrow(() -> ResourceNotFoundException.of("RfqLine", request.rfqLineId()));
        if (!line.getRfqId().equals(rfqId)) {
            throw new BusinessRuleViolationException("RFQ line does not belong to this RFQ", "M09-M10");
        }
        SupplierMaster supplier = supplierService.require(request.supplierId());
        if (supplier.getStatus() != SupplierStatus.ACTIVE) {
            throw new BusinessRuleViolationException("Supplier is not ACTIVE", "M09-M10");
        }
        if (request.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException("unitPrice must be >= 0", "M09-M10");
        }

        SupplierQuote q = new SupplierQuote();
        q.setQuoteId(UUID.randomUUID().toString());
        q.setCustomerId(tenantAccessGuard.requireCustomerId());
        q.setRfqId(rfqId);
        q.setRfqLineId(line.getRfqLineId());
        q.setSupplierId(supplier.getSupplierId());
        q.setBomNodeId(line.getBomNodeId());
        q.setUnitPrice(request.unitPrice());
        q.setCurrencyCode(request.currencyCode() == null || request.currencyCode().isBlank()
                ? rfq.getCurrencyCode() : request.currencyCode().trim().toUpperCase());
        q.setToolingLumpSum(request.toolingLumpSum());
        q.setLeadTimeDays(request.leadTimeDays());
        q.setValidFrom(request.validFrom());
        q.setValidTo(request.validTo());
        q.setStatus(QuoteStatus.SUBMITTED);
        q.setNegotiationReference(request.negotiationReference());
        q.setNotes(request.notes());

        SupplierQuote saved = quoteRepository.save(q);
        auditService.recordCreate(ENTITY, saved.getQuoteId(), toResponse(saved));
        publishQuote("QUOTE_RECEIVED", saved.getRfqId(), saved.getQuoteId(),
                "Quote received for RFQ " + saved.getRfqId());
        return toResponse(saved);
    }

    @Transactional
    public QuoteResponse updateStatus(String quoteId, UpdateQuoteStatusRequest request) {
        SupplierQuote q = require(quoteId);
        QuoteStatus next = request.status();

        if (next == QuoteStatus.ACCEPTED) {
            // Supersede other ACCEPTED quotes for same BOM node
            quoteRepository.findByBomNodeIdAndStatusOrderByUpdatedAtDesc(q.getBomNodeId(), QuoteStatus.ACCEPTED)
                    .stream()
                    .filter(existing -> !existing.getQuoteId().equals(q.getQuoteId()))
                    .forEach(existing -> {
                        existing.setStatus(QuoteStatus.SUPERSEDED);
                        quoteRepository.save(existing);
                    });
            RfqEvent rfq = rfqService.require(q.getRfqId());
            if (rfq.getStatus() == RfqStatus.OPEN) {
                rfq.setStatus(RfqStatus.AWARDED);
            }
            if (request.negotiationReference() != null && !request.negotiationReference().isBlank()) {
                q.setNegotiationReference(request.negotiationReference().trim());
            }
        }

        q.setStatus(next);
        SupplierQuote saved = quoteRepository.save(q);
        if (next == QuoteStatus.ACCEPTED) {
            costVersionService.snapshotAfterChange(
                    CostVersionService.TYPE_SUPPLIER_QUOTE,
                    saved.getQuoteId(),
                    ChangeTrigger.RFQ_ROUND,
                    saved.getRfqId(),
                    "Supplier quote ACCEPTED",
                    Map.of(
                            "quoteId", saved.getQuoteId(),
                            "bomNodeId", saved.getBomNodeId() != null ? saved.getBomNodeId() : "",
                            "status", "ACCEPTED",
                            "unitPrice", saved.getUnitPrice() != null ? saved.getUnitPrice().toPlainString() : ""
                    ));
        }
        if (next == QuoteStatus.ACCEPTED) {
            publishQuote("QUOTE_ACCEPTED", saved.getRfqId(), saved.getQuoteId(),
                    "Quote accepted " + saved.getQuoteId());
        } else if (next == QuoteStatus.REJECTED) {
            publishQuote("QUOTE_REJECTED", saved.getRfqId(), saved.getQuoteId(),
                    "Quote rejected " + saved.getQuoteId());
        }
        auditService.recordUpdate(ENTITY, saved.getQuoteId(), null, toResponse(saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<QuoteResponse> listByRfq(String rfqId) {
        rfqService.require(rfqId);
        assertWallAccess(rfqId);
        return quoteRepository.findByCustomerIdAndRfqIdOrderByCreatedAtDesc(tenantAccessGuard.requireCustomerId(), rfqId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuoteResponse> listByBomNode(String bomNodeId) {
        return quoteRepository.findByBomNodeIdAndStatusOrderByUpdatedAtDesc(bomNodeId, QuoteStatus.ACCEPTED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * M10: unit cost for a BUY leaf — latest ACCEPTED quote for the BOM node, else empty.
     */
    @Transactional(readOnly = true)
    public java.util.Optional<BigDecimal> acceptedUnitPriceForBomNode(String bomNodeId) {
        return quoteRepository.findFirstByBomNodeIdAndStatusOrderByUpdatedAtDesc(bomNodeId, QuoteStatus.ACCEPTED)
                .map(SupplierQuote::getUnitPrice);
    }

    public SupplierQuote require(String quoteId) {
        return quoteRepository.findByCustomerIdAndQuoteId(tenantAccessGuard.requireCustomerId(), quoteId)
                .orElseThrow(() -> ResourceNotFoundException.of(ENTITY, quoteId));
    }

    /**
     * SEC-04: if walls exist for this RFQ, caller must match allowed role or user.
     * No walls configured → open to authenticated roles that can call the API.
     */
    private void assertWallAccess(String rfqId) {
        List<ConfidentialityWall> walls = wallRepository.findByRfqEventReference(rfqId);
        if (walls == null || walls.isEmpty()) {
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessRuleViolationException("Confidentiality wall: authentication required", "M09-M10");
        }
        Set<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toSet());
        String userId = auth.getName();
        boolean allowed = walls.stream().anyMatch(w ->
                (w.getAllowedRoleCode() != null && roles.contains(w.getAllowedRoleCode()))
                        || (w.getAllowedUserId() != null && w.getAllowedUserId().equals(userId))
                        || roles.contains("ADMIN")
                        || roles.contains("SYSTEM_ADMINISTRATOR"));
        if (!allowed) {
            throw new BusinessRuleViolationException(
                    "Confidentiality wall blocks access to quotes for RFQ " + rfqId, "M09-M10");
        }
    }

    private QuoteResponse toResponse(SupplierQuote q) {
        return new QuoteResponse(
                q.getQuoteId(), q.getRfqId(), q.getRfqLineId(), q.getSupplierId(), q.getBomNodeId(),
                q.getUnitPrice(), q.getCurrencyCode(), q.getToolingLumpSum(), q.getLeadTimeDays(),
                q.getValidFrom(), q.getValidTo(), q.getStatus(), q.getNegotiationReference(), q.getNotes());
    }

    private void publishQuote(String kind, String rfqId, String quoteId, String summary) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return;
        }
        eventPublisher.publishEvent(new RfqLifecycleEvent(kind, rfqId, quoteId, auth.getName(), summary));
    }
}
