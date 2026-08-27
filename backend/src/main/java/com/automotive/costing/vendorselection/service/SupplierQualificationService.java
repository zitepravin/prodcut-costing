package com.automotive.costing.vendorselection.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.engine.SupplierLifecycleTransitionEngine;
import com.automotive.costing.vendorselection.entity.RecommendationStatus;
import com.automotive.costing.vendorselection.entity.SupplierLifecycleStatus;
import com.automotive.costing.vendorselection.entity.SupplierQualification;
import com.automotive.costing.vendorselection.entity.SupplierQualificationApprovalCheck;
import com.automotive.costing.vendorselection.entity.SupplierQualificationCurrent;
import com.automotive.costing.vendorselection.entity.VendorRecommendation;
import com.automotive.costing.vendorselection.repository.SupplierQualificationApprovalCheckRepository;
import com.automotive.costing.vendorselection.repository.SupplierQualificationLifecycleEventRepository;
import com.automotive.costing.vendorselection.entity.SupplierQualificationLifecycleEvent;
import com.automotive.costing.common.event.SupplierQualificationLifecycleEventMessage;
import org.springframework.context.ApplicationEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import com.automotive.costing.vendorselection.repository.SupplierQualificationCurrentRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.vendorselection.repository.SupplierQualificationRepository;
import com.automotive.costing.vendorselection.repository.VendorRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Supplier qualification lifecycle. APPROVED requires human reason and a linked
 * recommendation that is APPROVED or OVERRIDDEN for the same supplier (V56).
 * System never auto-approves.
 */
@Service
@RequiredArgsConstructor
public class SupplierQualificationService {

    private final SupplierQualificationRepository qualificationRepository;
    private final SupplierQualificationCurrentRepository currentRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final SupplierQualificationApprovalCheckRepository approvalCheckRepository;
    private final SupplierQualificationLifecycleEventRepository lifecycleEventRepository;
    private final VendorRecommendationRepository recommendationRepository;
    private final SupplierMasterService supplierMasterService;
    private final SupplierLifecycleTransitionEngine transitionEngine;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public record TransitionRequest(
            String supplierId,
            SupplierLifecycleStatus targetStatus,
            String reason,
            String recommendationId
    ) {}

    public record QualificationResponse(
            String qualificationId,
            String supplierId,
            String lifecycleStatus,
            String previousStatus,
            String changeReason,
            String recommendationId,
            Instant effectiveAt
    ) {}

    public record ApprovalCheckResponse(
            String checkId,
            String supplierId,
            String recommendationId,
            String recommendationStatus,
            String recommendedSupplierId,
            boolean checkPassed,
            String failureReason,
            Instant checkedAt
    ) {}

    @Transactional
    public QualificationResponse initialize(String supplierId) {
        supplierMasterService.require(supplierId);
        if (currentRepository.existsById(supplierId)) {
            throw new BusinessRuleViolationException(
                    "Qualification already initialized for supplier", "VS-LC-05");
        }
        return recordTransition(supplierId, null, SupplierLifecycleStatus.DISCOVERED,
                "Initial discovery", null);
    }

    @Transactional
    public QualificationResponse transition(TransitionRequest request) {
        if (request.supplierId() == null || request.targetStatus() == null) {
            throw new BusinessRuleViolationException("supplierId and targetStatus are required", "VS-LC-06");
        }
        supplierMasterService.require(request.supplierId());
        SupplierLifecycleStatus previous = currentRepository.findById(request.supplierId())
                .map(SupplierQualificationCurrent::getLifecycleStatus)
                .orElse(null);
        transitionEngine.assertTransitionAllowed(previous, request.targetStatus());

        if (request.targetStatus() == SupplierLifecycleStatus.APPROVED) {
            if (request.reason() == null || request.reason().isBlank()) {
                throw new BusinessRuleViolationException(
                        "Human approval reason is required for APPROVED", "VS-LC-07");
            }
            assertApprovedRecommendationGate(request.supplierId(), request.recommendationId());
        }
        if ((request.targetStatus() == SupplierLifecycleStatus.REJECTED
                || request.targetStatus() == SupplierLifecycleStatus.BLOCKED)
                && (request.reason() == null || request.reason().isBlank())) {
            throw new BusinessRuleViolationException(
                    "Reason is required for REJECTED or BLOCKED", "VS-LC-08");
        }
        if (request.targetStatus() == SupplierLifecycleStatus.RECOMMENDED
                || request.targetStatus() == SupplierLifecycleStatus.APPROVAL_PENDING) {
            if (request.recommendationId() == null || request.recommendationId().isBlank()) {
                throw new BusinessRuleViolationException(
                        "recommendationId is required for RECOMMENDED / APPROVAL_PENDING", "VS-LC-09");
            }
            // Ensure recommendation exists (may still be SUBMITTED at this stage)
            recommendationRepository.findById(request.recommendationId())
                    .orElseThrow(() -> ResourceNotFoundException.of(
                            "VendorRecommendation", request.recommendationId()));
        }

        return recordTransition(request.supplierId(), previous, request.targetStatus(),
                request.reason(), request.recommendationId());
    }

    /**
     * V56 gate: APPROVED lifecycle requires recommendation APPROVED or OVERRIDDEN
     * whose recommended supplier matches the qualification supplier.
     */
    private void assertApprovedRecommendationGate(String supplierId, String recommendationId) {
        if (recommendationId == null || recommendationId.isBlank()) {
            throw new BusinessRuleViolationException(
                    "recommendationId is required to move supplier to APPROVED", "VS-LC-10");
        }
        VendorRecommendation rec = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> ResourceNotFoundException.of("VendorRecommendation", recommendationId));

        String failure = null;
        boolean passed = true;
        if (rec.getStatus() != RecommendationStatus.APPROVED
                && rec.getStatus() != RecommendationStatus.OVERRIDDEN) {
            passed = false;
            failure = "Recommendation must be APPROVED or OVERRIDDEN (current=" + rec.getStatus() + ")";
        } else if (rec.getRecommendedSupplierId() == null
                || !rec.getRecommendedSupplierId().equals(supplierId)) {
            passed = false;
            failure = "Recommendation recommendedSupplierId must equal qualification supplierId";
        }

        SupplierQualificationApprovalCheck check = new SupplierQualificationApprovalCheck();
        check.setCheckId(UUID.randomUUID().toString());
        check.setSupplierId(supplierId);
        check.setRecommendationId(recommendationId);
        check.setRecommendationStatus(rec.getStatus().name());
        check.setRecommendedSupplierId(rec.getRecommendedSupplierId());
        check.setCheckPassed(passed);
        check.setFailureReason(failure);
        check.setCheckedAt(Instant.now());
        check.setCheckedBy(currentUserId());
        approvalCheckRepository.save(check);
        auditService.recordCreate("SupplierQualificationApprovalCheck", check.getCheckId(),
                passed ? "PASS" : "FAIL:" + failure);

        if (!passed) {
            throw new BusinessRuleViolationException(failure, "VS-LC-11");
        }
    }


    public record LifecycleEventResponse(
            String eventId,
            String qualificationId,
            String supplierId,
            String eventType,
            String previousStatus,
            String newStatus,
            String recommendationId,
            String changeReason,
            String payloadJson,
            boolean published,
            Instant publishedAt,
            Instant occurredAt,
            String occurredBy
    ) {}

    @Transactional(readOnly = true)
    public List<LifecycleEventResponse> listLifecycleEvents(String supplierId) {
        return lifecycleEventRepository.findBySupplierIdOrderByOccurredAtAsc(supplierId).stream()
                .map(this::toLifecycleEvent)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LifecycleEventResponse> listUnpublishedLifecycleEvents() {
        return lifecycleEventRepository.findByPublishedFlagFalseOrderByOccurredAtAsc().stream()
                .map(this::toLifecycleEvent)
                .toList();
    }

    @Transactional
    public LifecycleEventResponse markLifecycleEventPublished(String eventId) {
        SupplierQualificationLifecycleEvent ev = lifecycleEventRepository.findById(eventId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupplierQualificationLifecycleEvent", eventId));
        ev.setPublishedFlag(true);
        ev.setPublishedAt(Instant.now());
        lifecycleEventRepository.save(ev);
        auditService.recordUpdate("SupplierQualificationLifecycleEvent", eventId, "unpublished", "published");
        return toLifecycleEvent(ev);
    }

    private LifecycleEventResponse toLifecycleEvent(SupplierQualificationLifecycleEvent e) {
        return new LifecycleEventResponse(
                e.getEventId(),
                e.getQualificationId(),
                e.getSupplierId(),
                e.getEventType(),
                e.getPreviousStatus() == null ? null : e.getPreviousStatus().name(),
                e.getNewStatus().name(),
                e.getRecommendationId(),
                e.getChangeReason(),
                e.getPayloadJson(),
                e.isPublishedFlag(),
                e.getPublishedAt(),
                e.getOccurredAt(),
                e.getOccurredBy());
    }

    @Transactional(readOnly = true)
    public QualificationResponse current(String supplierId) {
        supplierMasterService.require(supplierId);
        SupplierQualificationCurrent cur = currentRepository.findById(supplierId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupplierQualificationCurrent", supplierId));
        if (cur.getCustomerId() != null) {
            tenantAccessGuard.assertOwns(cur.getCustomerId(), "SupplierQualificationCurrent", supplierId);
        }
        SupplierQualification q = qualificationRepository.findById(cur.getQualificationId())
                .orElseThrow(() -> ResourceNotFoundException.of("SupplierQualification", cur.getQualificationId()));
        return toResponse(q);
    }

    @Transactional(readOnly = true)
    public List<QualificationResponse> history(String supplierId) {
        supplierMasterService.require(supplierId);
        return qualificationRepository.findBySupplierIdOrderByEffectiveAtDesc(supplierId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApprovalCheckResponse> listApprovalChecks(String supplierId) {
        return approvalCheckRepository.findBySupplierIdOrderByCheckedAtDesc(supplierId).stream()
                .map(c -> new ApprovalCheckResponse(
                        c.getCheckId(),
                        c.getSupplierId(),
                        c.getRecommendationId(),
                        c.getRecommendationStatus(),
                        c.getRecommendedSupplierId(),
                        c.isCheckPassed(),
                        c.getFailureReason(),
                        c.getCheckedAt()))
                .toList();
    }

    private QualificationResponse recordTransition(
            String supplierId,
            SupplierLifecycleStatus previous,
            SupplierLifecycleStatus next,
            String reason,
            String recommendationId) {
        SupplierQualification q = new SupplierQualification();
        q.setQualificationId(UUID.randomUUID().toString());
        q.setSupplierId(supplierId);
        q.setLifecycleStatus(next);
        q.setPreviousStatus(previous);
        q.setChangeReason(reason);
        q.setRecommendationId(recommendationId);
        q.setEffectiveAt(Instant.now());
        q.setChangedBy(currentUserId());
        qualificationRepository.save(q);

        String customerId = tenantAccessGuard.requireCustomerId();
        SupplierQualificationCurrent cur = currentRepository.findById(supplierId).orElseGet(() -> {
            SupplierQualificationCurrent c = new SupplierQualificationCurrent();
            c.setSupplierId(supplierId);
            c.setCustomerId(customerId);
            return c;
        });
        if (cur.getCustomerId() == null || cur.getCustomerId().isBlank()) {
            cur.setCustomerId(customerId);
        } else {
            tenantAccessGuard.assertOwns(cur.getCustomerId(), "SupplierQualificationCurrent", supplierId);
        }
        cur.setQualificationId(q.getQualificationId());
        cur.setLifecycleStatus(next);
        cur.setUpdatedAt(Instant.now());
        currentRepository.save(cur);

        auditService.recordCreate("SupplierQualification", q.getQualificationId(),
                (previous == null ? "null" : previous.name()) + "->" + next.name());

        SupplierQualificationLifecycleEvent ev = new SupplierQualificationLifecycleEvent();
        ev.setEventId(UUID.randomUUID().toString());
        ev.setQualificationId(q.getQualificationId());
        ev.setSupplierId(supplierId);
        ev.setEventType("QUALIFICATION_" + next.name());
        ev.setPreviousStatus(previous);
        ev.setNewStatus(next);
        ev.setRecommendationId(recommendationId);
        ev.setChangeReason(reason);
        ev.setOccurredAt(q.getEffectiveAt());
        ev.setOccurredBy(q.getChangedBy());
        ev.setPublishedFlag(false);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("lifecycleStatus", next.name());
        payload.put("previousStatus", previous == null ? null : previous.name());
        payload.put("humanControlled", true);
        payload.put("autoApproveForbidden", true);
        try {
            ev.setPayloadJson(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            ev.setPayloadJson("{\"humanControlled\":true,\"autoApproveForbidden\":true}");
        }
        lifecycleEventRepository.save(ev);
        auditService.recordCreate("SupplierQualificationLifecycleEvent", ev.getEventId(), ev.getEventType());

        eventPublisher.publishEvent(new SupplierQualificationLifecycleEventMessage(
                ev.getEventId(),
                q.getQualificationId(),
                supplierId,
                ev.getEventType(),
                previous == null ? null : previous.name(),
                next.name(),
                recommendationId,
                ev.getOccurredAt(),
                ev.getOccurredBy()));

        return toResponse(q);
    }

    private QualificationResponse toResponse(SupplierQualification q) {
        return new QualificationResponse(
                q.getQualificationId(),
                q.getSupplierId(),
                q.getLifecycleStatus().name(),
                q.getPreviousStatus() == null ? null : q.getPreviousStatus().name(),
                q.getChangeReason(),
                q.getRecommendationId(),
                q.getEffectiveAt());
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.automotive.costing.platform.user.entity.User u) {
            return u.getUserId();
        }
        return auth != null ? auth.getName() : null;
    }
}
