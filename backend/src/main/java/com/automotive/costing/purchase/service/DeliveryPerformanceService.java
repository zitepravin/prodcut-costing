package com.automotive.costing.purchase.service;

import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.user.entity.ScopeType;
import com.automotive.costing.platform.user.service.AccessScopeService;
import com.automotive.costing.purchase.dto.DeliveryPerformanceDtos.OnTimeOverrideRequest;
import com.automotive.costing.purchase.dto.DeliveryPerformanceDtos.PerformanceSummary;
import com.automotive.costing.purchase.dto.DeliveryPerformanceDtos.ShipmentPerformance;
import com.automotive.costing.purchase.entity.PurchaseOrderTracking;
import com.automotive.costing.purchase.entity.PurchaseRequest;
import com.automotive.costing.purchase.entity.PurchaseRequestLine;
import com.automotive.costing.purchase.repository.PurchaseOrderTrackingRepository;
import com.automotive.costing.purchase.repository.PurchaseRequestLineRepository;
import com.automotive.costing.purchase.repository.PurchaseRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * P3-06 Option A — delivery performance from order tracking + PR lines.
 * Missing need-by / ASN / qty → DATA NOT AVAILABLE (never invent).
 * Human on-time override requires reason. No SAP writes. No AI auto-penalty.
 */
@Service
@RequiredArgsConstructor
public class DeliveryPerformanceService {

    public static final String NOT_AVAILABLE = "NOT_AVAILABLE";
    public static final String AVAILABLE = "AVAILABLE";

    private final PurchaseRequestRepository requestRepository;
    private final PurchaseRequestLineRepository lineRepository;
    private final PurchaseOrderTrackingRepository trackingRepository;
    private final AccessScopeService accessScopeService;

    @Transactional(readOnly = true)
    public PerformanceSummary summarize(String purchaseRequestId) {
        PurchaseRequest pr = requirePr(purchaseRequestId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, pr.getProgramId());

        Map<String, PurchaseRequestLine> linesById = new HashMap<>();
        for (PurchaseRequestLine line : lineRepository.findByPurchaseRequestIdOrderByLineNoAsc(purchaseRequestId)) {
            linesById.put(line.getLineId(), line);
        }

        List<ShipmentPerformance> shipments = new ArrayList<>();
        int onTime = 0, late = 0, dna = 0;

        for (PurchaseOrderTracking t : trackingRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequestId)) {
            ShipmentPerformance sp = evaluate(t, linesById);
            shipments.add(sp);
            if (NOT_AVAILABLE.equals(sp.dataStatus()) || sp.onTimeEffective() == null) {
                dna++;
            } else if (Boolean.TRUE.equals(sp.onTimeEffective())) {
                onTime++;
            } else {
                late++;
            }
        }

        return new PerformanceSummary(
                pr.getPurchaseRequestId(),
                pr.getProgramId(),
                pr.getStatus(),
                shipments.size(),
                onTime,
                late,
                dna,
                shipments,
                List.of(
                        "P3-06 delivery performance is calculated from tracking + PR lines.",
                        "Missing ASN, need-by, or qty → DATA NOT AVAILABLE (not zero).",
                        "Human on-time override requires reason. No SAP write. AI must not auto-penalize."
                )
        );
    }

    @Transactional
    public ShipmentPerformance overrideOnTime(String purchaseRequestId, String trackingId, OnTimeOverrideRequest request) {
        PurchaseRequest pr = requirePr(purchaseRequestId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, pr.getProgramId());

        if (request.reason() == null || request.reason().isBlank()) {
            throw new IllegalArgumentException("reason is required for on-time override");
        }
        if (request.onTimeOverride() == null) {
            throw new IllegalArgumentException("onTimeOverride true/false is required");
        }

        PurchaseOrderTracking t = trackingRepository.findById(trackingId)
                .orElseThrow(() -> new ResourceNotFoundException("Order tracking", trackingId));
        if (!purchaseRequestId.equals(t.getPurchaseRequestId())) {
            throw new IllegalArgumentException("Tracking row does not belong to this purchase request");
        }

        t.setOnTimeOverride(request.onTimeOverride());
        t.setOnTimeOverrideReason(request.reason().trim());
        t.setOnTimeOverrideBy(currentUserId());
        t.setOnTimeOverrideAt(Instant.now());
        trackingRepository.save(t);

        Map<String, PurchaseRequestLine> linesById = new HashMap<>();
        for (PurchaseRequestLine line : lineRepository.findByPurchaseRequestIdOrderByLineNoAsc(purchaseRequestId)) {
            linesById.put(line.getLineId(), line);
        }
        return evaluate(t, linesById);
    }

    ShipmentPerformance evaluate(PurchaseOrderTracking t, Map<String, PurchaseRequestLine> linesById) {
        PurchaseRequestLine line = t.getPurchaseRequestLineId() != null
                ? linesById.get(t.getPurchaseRequestLineId())
                : null;

        // Supplier from line when linked; otherwise unknown
        String supplierId = line != null ? line.getSupplierId() : null;
        LocalDate needBy = line != null ? line.getNeedByDate() : null;

        BigDecimal qtyOrdered = t.getQtyOrdered();
        if (qtyOrdered == null && line != null) {
            qtyOrdered = line.getQuantity();
        }
        BigDecimal qtyShipped = t.getQtyShipped();
        LocalDate asn = t.getAsnDate();

        List<String> notes = new ArrayList<>();
        String dataStatus = AVAILABLE;
        Long delayDays = null;
        Boolean onTimeComputed = null;

        if (asn == null || needBy == null) {
            dataStatus = NOT_AVAILABLE;
            notes.add("ASN date or need-by date missing — on-time not computed");
        } else {
            delayDays = ChronoUnit.DAYS.between(needBy, asn);
            onTimeComputed = delayDays <= 0;
        }

        Boolean onTimeEffective = onTimeComputed;
        boolean overrideApplied = false;
        if (t.getOnTimeOverride() != null) {
            onTimeEffective = t.getOnTimeOverride();
            overrideApplied = true;
            notes.add("Human on-time override applied");
        }

        String qtyVarStatus = AVAILABLE;
        BigDecimal qtyVariance = null;
        if (qtyOrdered == null || qtyShipped == null) {
            qtyVarStatus = NOT_AVAILABLE;
            notes.add("Qty ordered or shipped missing — qty variance NOT_AVAILABLE");
        } else {
            qtyVariance = qtyShipped.subtract(qtyOrdered);
        }

        if (supplierId == null) {
            notes.add("Supplier not resolved (tracking line not linked to PR line)");
        }

        return new ShipmentPerformance(
                t.getTrackingId(),
                t.getPurchaseRequestId(),
                t.getPurchaseRequestLineId(),
                supplierId,
                t.getTrackingStatus(),
                needBy,
                asn,
                qtyOrdered,
                qtyShipped,
                delayDays,
                onTimeComputed,
                onTimeEffective,
                overrideApplied,
                t.getOnTimeOverrideReason(),
                dataStatus,
                qtyVariance,
                qtyVarStatus,
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
}
