package com.automotive.costing.vendorselection.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.entity.*;
import com.automotive.costing.vendorselection.engine.EvidenceStatusTransitionEngine;
import com.automotive.costing.vendorselection.repository.SupplierDeliveryMetricRepository;
import com.automotive.costing.vendorselection.repository.SupplierPerformanceFeedbackRepository;
import com.automotive.costing.vendorselection.repository.SupplierQualityMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * C3 foundation: capture actual supplier performance feedback for history.
 * Apply-to-history may create a quality/delivery metric row when metric data is complete.
 * Never mutates scorecards or recommendations.
 */
@Service
@RequiredArgsConstructor
public class SupplierPerformanceFeedbackService {

    private final SupplierPerformanceFeedbackRepository feedbackRepository;
    private final SupplierQualityMetricRepository qualityRepository;
    private final SupplierDeliveryMetricRepository deliveryRepository;
    private final SupplierMasterService supplierMasterService;
    private final AuditService auditService;
    private final EvidenceStatusTransitionEngine evidenceStatusTransitionEngine;

    public record CreateFeedbackRequest(
            String supplierId, String bomNodeId, String programId, LocalDate feedbackDate,
            PerformanceFeedbackType feedbackType, LocalDate periodStart, LocalDate periodEnd,
            String metricCode, BigDecimal metricValue, String unit, String narrative,
            String sourceReference, String evidenceRef, String notes) {}

    public record FeedbackResponse(
            String feedbackId, String supplierId, String bomNodeId, String programId,
            LocalDate feedbackDate, PerformanceFeedbackType feedbackType, LocalDate periodStart,
            LocalDate periodEnd, String metricCode, BigDecimal metricValue, String unit,
            String narrative, String sourceReference, EvidenceStatus evidenceStatus,
            String evidenceRef, String verifiedBy, Instant verifiedAt, boolean appliedToHistory,
            Instant appliedAt, String historyMetricId, String historyMetricKind, String notes) {}

    public record VerifyFeedbackRequest(EvidenceStatus evidenceStatus, String evidenceRef) {}

    @Transactional
    public FeedbackResponse create(CreateFeedbackRequest r) {
        supplierMasterService.require(r.supplierId());
        if (r.feedbackDate() == null) throw new BusinessRuleViolationException("feedbackDate is required", "VS-PF-01");
        if (r.feedbackType() == null) throw new BusinessRuleViolationException("feedbackType is required", "VS-PF-02");
        if (r.periodStart() != null && r.periodEnd() != null && r.periodEnd().isBefore(r.periodStart())) {
            throw new BusinessRuleViolationException("periodEnd must be on or after periodStart", "VS-PF-03");
        }
        SupplierPerformanceFeedback f = new SupplierPerformanceFeedback();
        f.setFeedbackId(UUID.randomUUID().toString());
        f.setSupplierId(r.supplierId());
        f.setBomNodeId(r.bomNodeId());
        f.setProgramId(r.programId());
        f.setFeedbackDate(r.feedbackDate());
        f.setFeedbackType(r.feedbackType());
        f.setPeriodStart(r.periodStart());
        f.setPeriodEnd(r.periodEnd());
        f.setMetricCode(r.metricCode());
        f.setMetricValue(r.metricValue());
        f.setUnit(r.unit());
        f.setNarrative(r.narrative());
        f.setSourceReference(r.sourceReference());
        f.setEvidenceStatus(EvidenceStatus.PENDING);
        f.setEvidenceRef(r.evidenceRef());
        f.setAppliedToHistory(false);
        f.setNotes(r.notes());
        feedbackRepository.save(f);
        auditService.recordCreate("SupplierPerformanceFeedback", f.getFeedbackId(), r.feedbackType().name());
        return toResponse(f);
    }

    @Transactional
    public FeedbackResponse verify(String feedbackId, VerifyFeedbackRequest req) {
        SupplierPerformanceFeedback f = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupplierPerformanceFeedback", feedbackId));
        if (req == null || req.evidenceStatus() == null) {
            throw new BusinessRuleViolationException("evidenceStatus is required", "VS-PF-04");
        }
        EvidenceStatus previous = f.getEvidenceStatus();
        evidenceStatusTransitionEngine.assertTransitionAllowed(previous, req.evidenceStatus());
        f.setEvidenceStatus(req.evidenceStatus());
        if (req.evidenceRef() != null) f.setEvidenceRef(req.evidenceRef());
        f.setVerifiedBy(currentUserId());
        f.setVerifiedAt(Instant.now());
        feedbackRepository.save(f);
        auditService.recordVerify("SupplierPerformanceFeedback", feedbackId,
                previous != null ? previous.name() : null, req.evidenceStatus().name());
        return toResponse(f);
    }

    /**
     * Mark feedback applied and, when data is sufficient, create a history metric row.
     * Scorecards and recommendations are never modified here.
     */
    @Transactional
    public FeedbackResponse applyToHistory(String feedbackId) {
        SupplierPerformanceFeedback f = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupplierPerformanceFeedback", feedbackId));
        if (f.getEvidenceStatus() != EvidenceStatus.VERIFIED && f.getEvidenceStatus() != EvidenceStatus.WAIVED) {
            throw new BusinessRuleViolationException(
                    "Only VERIFIED or WAIVED feedback can be applied to history", "VS-PF-05");
        }
        if (f.isAppliedToHistory()) {
            return toResponse(f);
        }

        String metricId = null;
        String metricKind = null;
        LocalDate periodStart = f.getPeriodStart() != null ? f.getPeriodStart() : f.getFeedbackDate();
        LocalDate periodEnd = f.getPeriodEnd() != null ? f.getPeriodEnd() : f.getFeedbackDate();

        if (f.getFeedbackType() == PerformanceFeedbackType.QUALITY
                || f.getFeedbackType() == PerformanceFeedbackType.WARRANTY) {
            QualityMetricType qType = mapQualityType(f.getMetricCode(), f.getFeedbackType());
            if (qType != null && f.getMetricValue() != null) {
                SupplierQualityMetric m = new SupplierQualityMetric();
                m.setMetricId(UUID.randomUUID().toString());
                m.setSupplierId(f.getSupplierId());
                m.setMetricType(qType);
                m.setPeriodStart(periodStart);
                m.setPeriodEnd(periodEnd);
                m.setMetricValue(f.getMetricValue());
                m.setUnit(f.getUnit());
                m.setEvidenceStatus(EvidenceStatus.VERIFIED);
                m.setEvidenceRef(f.getEvidenceRef());
                m.setVerifiedBy(currentUserId());
                m.setVerifiedAt(Instant.now());
                m.setNotes(trimNotes("From performance feedback " + f.getFeedbackId(), f.getNarrative()));
                qualityRepository.save(m);
                auditService.recordCreate("SupplierQualityMetric", m.getMetricId(), "from_feedback:" + f.getFeedbackId());
                metricId = m.getMetricId();
                metricKind = "QUALITY";
            }
        } else if (f.getFeedbackType() == PerformanceFeedbackType.DELIVERY && f.getMetricValue() != null) {
            String code = f.getMetricCode() == null ? "" : f.getMetricCode().trim().toUpperCase(Locale.ROOT);
            SupplierDeliveryMetric m = new SupplierDeliveryMetric();
            m.setMetricId(UUID.randomUUID().toString());
            m.setSupplierId(f.getSupplierId());
            m.setPeriodStart(periodStart);
            m.setPeriodEnd(periodEnd);
            if ("OTD".equals(code) || "OTD_PERCENT".equals(code)) {
                m.setOtdPercent(f.getMetricValue());
            } else if ("OTIF".equals(code) || "OTIF_PERCENT".equals(code)) {
                m.setOtifPercent(f.getMetricValue());
            } else if ("LATE_COUNT".equals(code) || "LATE_DELIVERY_COUNT".equals(code)) {
                m.setLateDeliveryCount(f.getMetricValue().intValue());
            } else if ("AVG_DELAY".equals(code) || "AVG_DELAY_DAYS".equals(code)) {
                m.setAvgDelayDays(f.getMetricValue());
            } else {
                // Incomplete mapping — still mark applied without inventing delivery fields
                m = null;
            }
            if (m != null) {
                m.setEvidenceStatus(EvidenceStatus.VERIFIED);
                m.setEvidenceRef(f.getEvidenceRef());
                m.setVerifiedBy(currentUserId());
                m.setVerifiedAt(Instant.now());
                m.setNotes(trimNotes("From performance feedback " + f.getFeedbackId(), f.getNarrative()));
                deliveryRepository.save(m);
                auditService.recordCreate("SupplierDeliveryMetric", m.getMetricId(), "from_feedback:" + f.getFeedbackId());
                metricId = m.getMetricId();
                metricKind = "DELIVERY";
            }
        }

        f.setAppliedToHistory(true);
        f.setAppliedAt(Instant.now());
        f.setHistoryMetricId(metricId);
        f.setHistoryMetricKind(metricKind);
        feedbackRepository.save(f);
        auditService.recordUpdate("SupplierPerformanceFeedback", feedbackId, "not_applied",
                "applied_to_history" + (metricKind != null ? ":" + metricKind : ":flag_only"));
        return toResponse(f);
    }

    private static QualityMetricType mapQualityType(String metricCode, PerformanceFeedbackType type) {
        if (type == PerformanceFeedbackType.WARRANTY) {
            return QualityMetricType.WARRANTY_CLAIMS;
        }
        if (metricCode == null || metricCode.isBlank()) {
            return null;
        }
        String c = metricCode.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return switch (c) {
            case "PPM" -> QualityMetricType.PPM;
            case "REJECTION_RATE", "REJECTION" -> QualityMetricType.REJECTION_RATE;
            case "NCR", "NCR_COUNT" -> QualityMetricType.NCR_COUNT;
            case "COMPLAINT", "COMPLAINT_COUNT", "CUSTOMER_COMPLAINT" -> QualityMetricType.COMPLAINT_COUNT;
            case "CAPA_OPEN" -> QualityMetricType.CAPA_OPEN;
            case "CAPA_OVERDUE" -> QualityMetricType.CAPA_OVERDUE;
            case "AUDIT_SCORE", "AUDIT" -> QualityMetricType.AUDIT_SCORE;
            case "FPY", "FIRST_PASS_YIELD" -> QualityMetricType.FPY;
            case "WARRANTY_CLAIMS", "WARRANTY" -> QualityMetricType.WARRANTY_CLAIMS;
            default -> null;
        };
    }

    private static String trimNotes(String prefix, String narrative) {
        String n = prefix + (narrative != null && !narrative.isBlank() ? " — " + narrative : "");
        return n.length() > 1000 ? n.substring(0, 1000) : n;
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> listBySupplier(String supplierId) {
        return listBySupplier(supplierId, null, null);
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> listBySupplier(String supplierId, Boolean appliedToHistory, EvidenceStatus evidenceStatus) {
        supplierMasterService.require(supplierId);
        java.util.List<SupplierPerformanceFeedback> rows;
        if (appliedToHistory != null) {
            rows = feedbackRepository.findBySupplierIdAndAppliedToHistoryOrderByFeedbackDateDesc(supplierId, appliedToHistory);
        } else if (evidenceStatus != null) {
            rows = feedbackRepository.findBySupplierIdAndEvidenceStatusOrderByFeedbackDateDesc(supplierId, evidenceStatus);
        } else {
            rows = feedbackRepository.findBySupplierIdOrderByFeedbackDateDesc(supplierId);
        }
        return rows.stream().map(this::toResponse).toList();
    }

    private FeedbackResponse toResponse(SupplierPerformanceFeedback f) {
        return new FeedbackResponse(
                f.getFeedbackId(), f.getSupplierId(), f.getBomNodeId(), f.getProgramId(),
                f.getFeedbackDate(), f.getFeedbackType(), f.getPeriodStart(), f.getPeriodEnd(),
                f.getMetricCode(), f.getMetricValue(), f.getUnit(), f.getNarrative(),
                f.getSourceReference(), f.getEvidenceStatus(), f.getEvidenceRef(),
                f.getVerifiedBy(), f.getVerifiedAt(), f.isAppliedToHistory(), f.getAppliedAt(),
                f.getHistoryMetricId(), f.getHistoryMetricKind(), f.getNotes());
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.automotive.costing.platform.user.entity.User u) {
            return u.getUserId();
        }
        return null;
    }
}
