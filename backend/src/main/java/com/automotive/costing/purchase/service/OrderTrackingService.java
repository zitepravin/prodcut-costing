package com.automotive.costing.purchase.service;

import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.user.entity.ScopeType;
import com.automotive.costing.platform.user.service.AccessScopeService;
import com.automotive.costing.purchase.dto.OrderTrackingDtos.HumanCorrectionRequest;
import com.automotive.costing.purchase.dto.OrderTrackingDtos.InboundTrackingRequest;
import com.automotive.costing.purchase.dto.OrderTrackingDtos.TrackingResponse;
import com.automotive.costing.purchase.entity.PurchaseOrderTracking;
import com.automotive.costing.purchase.entity.PurchaseRequest;
import com.automotive.costing.purchase.repository.PurchaseOrderTrackingRepository;
import com.automotive.costing.purchase.repository.PurchaseRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * P3-05 Order tracking — CPI inbound mirror after RELEASED PR.
 * No SAP writes. Human corrections require reason. Missing fields stay NOT_AVAILABLE.
 */
@Service
@RequiredArgsConstructor
public class OrderTrackingService {

    public static final String NOT_AVAILABLE = "NOT_AVAILABLE";
    public static final String AVAILABLE = "AVAILABLE";
    public static final String SOURCE_CPI = "CPI";

    private final PurchaseOrderTrackingRepository trackingRepository;
    private final PurchaseRequestRepository requestRepository;
    private final AccessScopeService accessScopeService;

    @Transactional
    public TrackingResponse ingestFromCpi(InboundTrackingRequest request) {
        PurchaseRequest pr = requirePr(request.purchaseRequestId());
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, pr.getProgramId());

        if (!"RELEASED".equals(pr.getStatus())) {
            throw new IllegalArgumentException(
                    "Order tracking ingest only allowed when purchase request is RELEASED (status="
                            + pr.getStatus() + ")");
        }

        PurchaseOrderTracking row = null;
        if (request.sourceEventId() != null && !request.sourceEventId().isBlank()) {
            row = trackingRepository
                    .findBySourceSystemAndSourceEventId(SOURCE_CPI, request.sourceEventId().trim())
                    .orElse(null);
        }
        if (row == null) {
            row = new PurchaseOrderTracking();
            row.setTrackingId(UUID.randomUUID().toString());
            row.setPurchaseRequestId(pr.getPurchaseRequestId());
            row.setSourceSystem(SOURCE_CPI);
        }

        row.setPurchaseRequestLineId(blankToNull(request.purchaseRequestLineId()));
        row.setExternalPoReference(firstNonBlank(request.externalPoReference(), pr.getExternalPoReference()));
        row.setTrackingStatus(normalizeStatus(request.trackingStatus()));
        row.setAsnDate(request.asnDate());
        row.setQtyShipped(request.qtyShipped());
        row.setQtyOrdered(request.qtyOrdered());
        row.setUom(blankToNull(request.uom()));
        row.setSourceEventId(blankToNull(request.sourceEventId()));
        row.setNotes(blankToNull(request.notes()));
        row.setDataStatus(computeDataStatus(row.getAsnDate(), row.getQtyShipped()));
        // CPI re-ingest clears human correction flag only when not human path
        if (!row.isHumanCorrected()) {
            row.setCorrectionReason(null);
            row.setCorrectedBy(null);
            row.setCorrectedAt(null);
        }
        trackingRepository.save(row);

        return toResponse(row, List.of(
                "CPI inbound stored as read-only mirror.",
                "This service does not write to SAP MM.",
                "Human corrections require correctionReason."
        ));
    }

    @Transactional(readOnly = true)
    public List<TrackingResponse> listForPurchaseRequest(String purchaseRequestId) {
        PurchaseRequest pr = requirePr(purchaseRequestId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, pr.getProgramId());
        return trackingRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequestId).stream()
                .map(t -> toResponse(t, List.of()))
                .toList();
    }

    @Transactional
    public TrackingResponse humanCorrect(String purchaseRequestId, String trackingId, HumanCorrectionRequest request) {
        PurchaseRequest pr = requirePr(purchaseRequestId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, pr.getProgramId());

        if (request.correctionReason() == null || request.correctionReason().isBlank()) {
            throw new IllegalArgumentException("correctionReason is required for human correction");
        }

        PurchaseOrderTracking row = trackingRepository.findById(trackingId)
                .orElseThrow(() -> new ResourceNotFoundException("Order tracking", trackingId));
        if (!purchaseRequestId.equals(row.getPurchaseRequestId())) {
            throw new IllegalArgumentException("Tracking row does not belong to this purchase request");
        }

        if (request.asnDate() != null) {
            row.setAsnDate(request.asnDate());
        }
        if (request.qtyShipped() != null) {
            row.setQtyShipped(request.qtyShipped());
        }
        if (request.trackingStatus() != null && !request.trackingStatus().isBlank()) {
            row.setTrackingStatus(normalizeStatus(request.trackingStatus()));
        }
        if (request.uom() != null && !request.uom().isBlank()) {
            row.setUom(request.uom().trim());
        }
        if (request.notes() != null) {
            row.setNotes(blankToNull(request.notes()));
        }

        row.setHumanCorrected(true);
        row.setCorrectionReason(request.correctionReason().trim());
        row.setCorrectedBy(currentUserId());
        row.setCorrectedAt(Instant.now());
        row.setDataStatus(computeDataStatus(row.getAsnDate(), row.getQtyShipped()));
        trackingRepository.save(row);

        return toResponse(row, List.of(
                "Human correction recorded by " + nullToDash(row.getCorrectedBy()) + ".",
                "No SAP write performed."
        ));
    }

    static String computeDataStatus(java.time.LocalDate asnDate, BigDecimal qtyShipped) {
        if (asnDate == null || qtyShipped == null) {
            return NOT_AVAILABLE;
        }
        return AVAILABLE;
    }

    private static String normalizeStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return "UNKNOWN";
        }
        String s = raw.trim().toUpperCase(Locale.ROOT);
        return switch (s) {
            case "OPEN", "PARTIAL", "SHIPPED", "DELIVERED", "CANCELLED", "UNKNOWN" -> s;
            default -> s.length() > 30 ? s.substring(0, 30) : s;
        };
    }

    private TrackingResponse toResponse(PurchaseOrderTracking t, List<String> notes) {
        return new TrackingResponse(
                t.getTrackingId(),
                t.getPurchaseRequestId(),
                t.getPurchaseRequestLineId(),
                t.getExternalPoReference(),
                t.getTrackingStatus(),
                t.getAsnDate(),
                t.getQtyShipped(),
                t.getQtyOrdered(),
                t.getUom(),
                t.getSourceSystem(),
                t.getSourceEventId(),
                t.getDataStatus(),
                t.isHumanCorrected(),
                t.getCorrectionReason(),
                t.getCorrectedBy(),
                t.getCorrectedAt(),
                t.getNotes(),
                notes
        );
    }

    private PurchaseRequest requirePr(String id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase request", id));
    }

    static String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null || auth.getName().isBlank()) {
            return null;
        }
        if ("anonymousUser".equalsIgnoreCase(auth.getName())) {
            return null;
        }
        return auth.getName();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a.trim();
        if (b != null && !b.isBlank()) return b.trim();
        return null;
    }

    private static String nullToDash(String s) {
        return s == null || s.isBlank() ? "(unknown user)" : s;
    }
}
