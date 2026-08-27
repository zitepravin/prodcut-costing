package com.automotive.costing.purchase.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.user.entity.ScopeType;
import com.automotive.costing.platform.user.service.AccessScopeService;
import com.automotive.costing.purchase.dto.SapPoLinkDtos.HumanPoLinkRequest;
import com.automotive.costing.purchase.dto.SapPoLinkDtos.SapPoLinkRequest;
import com.automotive.costing.purchase.dto.SapPoLinkDtos.SapPoLinkResponse;
import com.automotive.costing.purchase.entity.PurchasePoLinkEvent;
import com.automotive.costing.purchase.entity.PurchaseRequest;
import com.automotive.costing.purchase.repository.PurchasePoLinkEventRepository;
import com.automotive.costing.purchase.repository.PurchaseRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * P3-09a — CPI callback stores external SAP PO number after customer posts PO.
 * No SAP write from this service. Auth = application JWT (service account or user).
 */
@Service
@RequiredArgsConstructor
public class SapPoLinkService {

    public static final String NOT_LINKED = "NOT_LINKED";
    public static final String LINKED = "LINKED";
    public static final String SOURCE_CPI = "CPI_CALLBACK";
    public static final String SOURCE_HUMAN = "MANUAL_ADMIN";
    public static final String SYSTEM_SAP_MM = "SAP_MM";

    private final PurchaseRequestRepository requestRepository;
    private final PurchasePoLinkEventRepository eventRepository;
    private final AccessScopeService accessScopeService;

    @Transactional
    public SapPoLinkResponse linkFromCpi(String purchaseRequestId, SapPoLinkRequest request) {
        PurchaseRequest pr = requireReleased(purchaseRequestId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, pr.getProgramId());

        String poNumber = requirePoNumber(request.externalPoNumber());
        String system = blankTo(request.externalPoSystem(), SYSTEM_SAP_MM).toUpperCase(Locale.ROOT);
        String eventId = request.sourceEventId() == null || request.sourceEventId().isBlank()
                ? null : request.sourceEventId().trim();

        if (eventId != null) {
            var existing = eventRepository.findBySourceEventId(eventId);
            if (existing.isPresent()) {
                PurchasePoLinkEvent ev = existing.get();
                if (!purchaseRequestId.equals(ev.getPurchaseRequestId())) {
                    throw new BusinessRuleViolationException(
                            "sourceEventId already used for a different purchase request", "PO-LINK-01");
                }
                if (!poNumber.equals(ev.getExternalPoNumber())) {
                    throw new BusinessRuleViolationException(
                            "sourceEventId conflict: different PO number already recorded", "PO-LINK-02");
                }
                // idempotent success
                return toResponse(pr, List.of("Idempotent replay of sourceEventId=" + eventId));
            }
        }

        applyLink(pr, poNumber, system, SOURCE_CPI, eventId, request.notes(), request.postedAt());
        if (eventId != null) {
            saveEvent(pr.getPurchaseRequestId(), eventId, poNumber, system, LINKED, request.notes());
        }
        requestRepository.save(pr);

        return toResponse(pr, List.of(
                "SAP PO number stored from CPI callback.",
                "This service does not write to SAP MM."
        ));
    }

    /**
     * §11 decision 3 (resolved Yes): human may overwrite after CPI link with reason.
     */
    @Transactional
    public SapPoLinkResponse linkFromHuman(String purchaseRequestId, HumanPoLinkRequest request) {
        PurchaseRequest pr = requireReleased(purchaseRequestId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, pr.getProgramId());

        if (request.reason() == null || request.reason().isBlank()) {
            throw new IllegalArgumentException("reason is required for human PO link override");
        }
        String poNumber = requirePoNumber(request.externalPoNumber());
        String notes = "Human override: " + request.reason().trim()
                + (request.notes() == null || request.notes().isBlank() ? "" : " | " + request.notes().trim());

        applyLink(pr, poNumber, SYSTEM_SAP_MM, SOURCE_HUMAN, null, notes, Instant.now());
        requestRepository.save(pr);

        return toResponse(pr, List.of(
                "SAP PO number set by human admin with reason.",
                "This service does not write to SAP MM."
        ));
    }

    private void applyLink(
            PurchaseRequest pr,
            String poNumber,
            String system,
            String source,
            String eventId,
            String notes,
            Instant postedAt
    ) {
        pr.setExternalPoReference(poNumber);
        pr.setExternalPoSystem(system);
        pr.setPoLinkStatus(LINKED);
        pr.setPoLinkedAt(postedAt != null ? postedAt : Instant.now());
        pr.setPoLinkedSource(source);
        if (eventId != null) {
            pr.setPoLinkEventId(eventId);
        }
        pr.setPoLinkNotes(notes == null || notes.isBlank() ? null : notes.trim());
    }

    private void saveEvent(
            String prId, String sourceEventId, String poNumber, String system, String status, String notes
    ) {
        PurchasePoLinkEvent ev = new PurchasePoLinkEvent();
        ev.setEventId(UUID.randomUUID().toString());
        ev.setPurchaseRequestId(prId);
        ev.setSourceEventId(sourceEventId);
        ev.setExternalPoNumber(poNumber);
        ev.setExternalPoSystem(system);
        ev.setStatus(status);
        ev.setNotes(notes);
        ev.setReceivedAt(Instant.now());
        ev.setReceivedBy(currentUserId());
        eventRepository.save(ev);
    }

    private PurchaseRequest requireReleased(String id) {
        PurchaseRequest pr = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase request", id));
        if (!"RELEASED".equals(pr.getStatus())) {
            throw new BusinessRuleViolationException(
                    "SAP PO link only allowed when purchase request is RELEASED (status=" + pr.getStatus() + ")",
                    "PO-LINK-RELEASED");
        }
        return pr;
    }

    private static String requirePoNumber(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("externalPoNumber is required");
        }
        return raw.trim();
    }

    private SapPoLinkResponse toResponse(PurchaseRequest pr, List<String> notes) {
        return new SapPoLinkResponse(
                pr.getPurchaseRequestId(),
                pr.getStatus(),
                pr.getExternalPoReference(),
                pr.getExternalPoSystem(),
                pr.getPoLinkStatus() == null ? NOT_LINKED : pr.getPoLinkStatus(),
                pr.getPoLinkedAt(),
                pr.getPoLinkedSource(),
                pr.getPoLinkEventId(),
                notes
        );
    }

    private static String blankTo(String s, String def) {
        return s == null || s.isBlank() ? def : s.trim();
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
