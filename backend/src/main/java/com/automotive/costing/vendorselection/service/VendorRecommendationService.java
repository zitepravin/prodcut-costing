package com.automotive.costing.vendorselection.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.vendorselection.engine.RecommendationRankingEngine;
import com.automotive.costing.vendorselection.engine.RecommendationExplainabilityEngine;
import com.automotive.costing.vendorselection.engine.RecommendationExplainabilityEngine.ExplanationInput;
import com.automotive.costing.vendorselection.entity.VendorRecommendationExplanation;
import com.automotive.costing.vendorselection.repository.VendorRecommendationExplanationRepository;
import com.automotive.costing.vendorselection.repository.VendorRecommendationDecisionRepository;
import com.automotive.costing.vendorselection.repository.VendorRecommendationDecisionEventRepository;
import com.automotive.costing.vendorselection.entity.VendorRecommendationDecisionEvent;
import com.automotive.costing.common.event.VendorRecommendationDecisionEventMessage;
import org.springframework.context.ApplicationEventPublisher;
import java.util.LinkedHashMap;
import java.util.Map;
import com.automotive.costing.vendorselection.entity.VendorRecommendationDecision;
import com.automotive.costing.vendorselection.entity.RecommendationDecisionAction;
import com.automotive.costing.vendorselection.engine.RecommendationRankingEngine.CandidateInput;
import com.automotive.costing.vendorselection.entity.RecommendationStatus;
import com.automotive.costing.vendorselection.entity.VendorRecommendation;
import com.automotive.costing.vendorselection.entity.VendorRecommendationCandidate;
import com.automotive.costing.vendorselection.repository.VendorRecommendationCandidateRepository;
import com.automotive.costing.vendorselection.repository.VendorRecommendationRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.vendorselection.repository.SupplierQualificationCurrentRepository;
import com.automotive.costing.vendorselection.entity.SupplierQualificationCurrent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * B5 recommendation drafts and human decisions.
 * System never auto-approves; APPROVED/REJECTED/OVERRIDDEN require explicit human action.
 */
@Service
@RequiredArgsConstructor
public class VendorRecommendationService {

    private final VendorRecommendationRepository recommendationRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final VendorRecommendationCandidateRepository candidateRepository;
    private final SupplierQualificationCurrentRepository qualificationCurrentRepository;
    private final RecommendationRankingEngine rankingEngine;
    private final RecommendationExplainabilityEngine explainabilityEngine;
    private final VendorRecommendationExplanationRepository explanationRepository;
    private final VendorRecommendationDecisionRepository decisionRepository;
    private final VendorRecommendationDecisionEventRepository decisionEventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public record CandidateRequest(
            String supplierId,
            boolean eligible,
            BigDecimal overallScore,
            BigDecimal tcoTotal,
            String tcoCurrency,
            Boolean tcoDataComplete,
            List<String> failedGates,
            List<String> missingDataHints
    ) {}

    public record CreateRecommendationRequest(
            String sourcingEventCode,
            String bomNodeId,
            String programId,
            String templateId,
            List<CandidateRequest> candidates
    ) {}

    public record CandidateResponse(
            String candidateId,
            String supplierId,
            int rankPosition,
            boolean eligible,
            BigDecimal overallScore,
            BigDecimal tcoTotal,
            String tcoCurrency,
            Boolean tcoDataComplete,
            String failedGates,
            String strengths,
            String weaknesses,
            String lifecycleStatus,
            boolean lifecycleBlocked
    ) {}

    public record RecommendationResponse(
            String recommendationId,
            String sourcingEventCode,
            String bomNodeId,
            String programId,
            String templateId,
            String status,
            String recommendedSupplierId,
            String rationale,
            String confidenceLevel,
            String missingDataSummary,
            List<CandidateResponse> ranking,
            Instant submittedAt,
            Instant decidedAt,
            String decisionComment,
            String overrideReason
    ) {}

    public record DecisionRequest(String comment, String overrideSupplierId, String overrideReason) {}

    @Transactional
    public RecommendationResponse createDraft(CreateRecommendationRequest request) {
        if (request.candidates() == null || request.candidates().isEmpty()) {
            throw new BusinessRuleViolationException("At least one candidate is required", "VS-REC-01");
        }
        List<CandidateInput> inputs = request.candidates().stream()
                .map(c -> {
                    String lifecycle = qualificationCurrentRepository.findById(c.supplierId())
                            .map(SupplierQualificationCurrent::getLifecycleStatus)
                            .map(Enum::name)
                            .orElse(null);
                    return new CandidateInput(
                            c.supplierId(),
                            c.eligible(),
                            c.overallScore(),
                            c.tcoTotal(),
                            Boolean.TRUE.equals(c.tcoDataComplete()),
                            c.failedGates(),
                            c.missingDataHints(),
                            lifecycle);
                })
                .toList();
        var ranking = rankingEngine.rank(inputs);

        VendorRecommendation rec = new VendorRecommendation();
        rec.setRecommendationId(UUID.randomUUID().toString());
        rec.setCustomerId(tenantAccessGuard.requireCustomerId());
        rec.setSourcingEventCode(request.sourcingEventCode());
        rec.setBomNodeId(request.bomNodeId());
        rec.setProgramId(request.programId());
        rec.setTemplateId(request.templateId());
        rec.setStatus(RecommendationStatus.DRAFT);
        rec.setRecommendedSupplierId(ranking.recommendedSupplierId());
        rec.setRationale(ranking.rationale());
        rec.setConfidenceLevel(ranking.confidenceLevel());
        rec.setMissingDataSummary(ranking.missingDataSummary());
        try {
            rec.setRankingJson(objectMapper.writeValueAsString(ranking.ranking()));
        } catch (JsonProcessingException e) {
            rec.setRankingJson("[]");
        }
        recommendationRepository.save(rec);

        for (var r : ranking.ranking()) {
            VendorRecommendationCandidate cand = new VendorRecommendationCandidate();
            cand.setCandidateId(UUID.randomUUID().toString());
            cand.setRecommendationId(rec.getRecommendationId());
            cand.setSupplierId(r.supplierId());
            cand.setRankPosition(r.rankPosition());
            cand.setEligible(r.eligible());
            cand.setOverallScore(r.overallScore());
            cand.setTcoTotal(r.tcoTotal());
            CandidateRequest original = request.candidates().stream()
                    .filter(c -> c.supplierId().equals(r.supplierId())).findFirst().orElse(null);
            if (original != null) {
                cand.setTcoCurrency(original.tcoCurrency());
                cand.setTcoDataComplete(original.tcoDataComplete());
            }
            cand.setFailedGates(r.failedGates() == null || r.failedGates().isEmpty()
                    ? null : String.join(",", r.failedGates()));
            cand.setStrengths(r.strengths());
            cand.setWeaknesses(r.weaknesses());
            cand.setLifecycleStatus(r.lifecycleStatus());
            cand.setLifecycleBlocked(r.lifecycleBlocked());
            candidateRepository.save(cand);
        }

        var explanation = explainabilityEngine.build(new ExplanationInput(
                ranking.recommendedSupplierId(),
                ranking.confidenceLevel(),
                ranking.rationale(),
                ranking.missingDataSummary(),
                ranking.ranking()));
        VendorRecommendationExplanation exp = new VendorRecommendationExplanation();
        exp.setExplanationId(UUID.randomUUID().toString());
        exp.setRecommendationId(rec.getRecommendationId());
        exp.setRecommendedSupplierId(ranking.recommendedSupplierId());
        exp.setConfidenceLevel(ranking.confidenceLevel());
        exp.setDataUsedSummary(explanation.dataUsedSummary());
        exp.setEligibilitySummary(explanation.eligibilitySummary());
        exp.setScoreSummary(explanation.scoreSummary());
        exp.setTcoSummary(explanation.tcoSummary());
        exp.setStrengthsSummary(explanation.strengthsSummary());
        exp.setWeaknessesSummary(explanation.weaknessesSummary());
        exp.setRiskSummary(explanation.riskSummary());
        exp.setMissingDataSummary(explanation.missingDataSummary());
        exp.setCostVarianceNotes(explanation.costVarianceNotes());
        exp.setNegotiationOpportunities(explanation.negotiationOpportunities());
        exp.setEvidenceNotes(explanation.evidenceNotes());
        exp.setDecisionBoundaryNote(explanation.decisionBoundaryNote());
        try {
            exp.setFactJson(objectMapper.writeValueAsString(explanation.facts()));
            exp.setCalculatedJson(objectMapper.writeValueAsString(explanation.calculated()));
            exp.setInferenceJson(objectMapper.writeValueAsString(explanation.inferences()));
            exp.setMissingJson(objectMapper.writeValueAsString(explanation.missing()));
        } catch (JsonProcessingException e) {
            exp.setFactJson("{}");
            exp.setCalculatedJson("{}");
            exp.setInferenceJson("{}");
            exp.setMissingJson("{}");
        }
        explanationRepository.save(exp);
        auditService.recordCreate("VendorRecommendationExplanation", exp.getExplanationId(),
                rec.getRecommendationId());

        auditService.recordCreate("VendorRecommendation", rec.getRecommendationId(),
                "DRAFT recommended=" + ranking.recommendedSupplierId());
        return toResponse(rec);
    }

    @Transactional
    public RecommendationResponse submit(String recommendationId) {
        VendorRecommendation rec = require(recommendationId);
        if (rec.getStatus() != RecommendationStatus.DRAFT) {
            throw new BusinessRuleViolationException("Only DRAFT recommendations can be submitted", "VS-REC-02");
        }
        rec.setStatus(RecommendationStatus.SUBMITTED);
        rec.setSubmittedAt(Instant.now());
        rec.setSubmittedBy(currentUserId());
        recommendationRepository.save(rec);
        recordDecision(rec, RecommendationStatus.DRAFT, RecommendationStatus.SUBMITTED,
                RecommendationDecisionAction.SUBMIT, null, null, null);

        auditService.recordTransition("VendorRecommendation", recommendationId, "DRAFT", "SUBMITTED");
        return toResponse(rec);
    }

    @Transactional
    public RecommendationResponse approve(String recommendationId, DecisionRequest request) {
        VendorRecommendation rec = require(recommendationId);
        if (rec.getStatus() != RecommendationStatus.SUBMITTED) {
            throw new BusinessRuleViolationException("Only SUBMITTED recommendations can be approved", "VS-REC-03");
        }
        if (rec.getRecommendedSupplierId() == null) {
            throw new BusinessRuleViolationException(
                    "No eligible recommended supplier — use reject or override with reason", "VS-REC-04");
        }
        rec.setStatus(RecommendationStatus.APPROVED);
        rec.setDecidedAt(Instant.now());
        rec.setDecidedBy(currentUserId());
        rec.setDecisionComment(request == null ? null : request.comment());
        recommendationRepository.save(rec);
        recordDecision(rec, RecommendationStatus.SUBMITTED, RecommendationStatus.APPROVED,
                RecommendationDecisionAction.APPROVE,
                request == null ? null : request.comment(), null, null);
        auditService.recordTransition("VendorRecommendation", recommendationId, "SUBMITTED", "APPROVED");
        return toResponse(rec);
    }

    @Transactional
    public RecommendationResponse reject(String recommendationId, DecisionRequest request) {
        VendorRecommendation rec = require(recommendationId);
        if (rec.getStatus() != RecommendationStatus.SUBMITTED) {
            throw new BusinessRuleViolationException("Only SUBMITTED recommendations can be rejected", "VS-REC-05");
        }
        rec.setStatus(RecommendationStatus.REJECTED);
        rec.setDecidedAt(Instant.now());
        rec.setDecidedBy(currentUserId());
        rec.setDecisionComment(request == null ? null : request.comment());
        recommendationRepository.save(rec);
        recordDecision(rec, RecommendationStatus.SUBMITTED, RecommendationStatus.REJECTED,
                RecommendationDecisionAction.REJECT,
                request == null ? null : request.comment(), null, null);
        auditService.recordTransition("VendorRecommendation", recommendationId, "SUBMITTED", "REJECTED");
        return toResponse(rec);
    }

    /**
     * Human override: choose a different supplier (including ineligible with mandatory reason).
     */
    @Transactional
    public RecommendationResponse override(String recommendationId, DecisionRequest request) {
        VendorRecommendation rec = require(recommendationId);
        if (rec.getStatus() != RecommendationStatus.SUBMITTED && rec.getStatus() != RecommendationStatus.DRAFT) {
            throw new BusinessRuleViolationException("Override only from DRAFT or SUBMITTED", "VS-REC-06");
        }
        if (request == null || request.overrideSupplierId() == null || request.overrideSupplierId().isBlank()) {
            throw new BusinessRuleViolationException("overrideSupplierId is required", "VS-REC-07");
        }
        if (request.overrideReason() == null || request.overrideReason().isBlank()) {
            throw new BusinessRuleViolationException("overrideReason is required for human override", "VS-REC-08");
        }
        RecommendationStatus previous = rec.getStatus();
        rec.setStatus(RecommendationStatus.OVERRIDDEN);
        rec.setRecommendedSupplierId(request.overrideSupplierId());
        rec.setOverrideReason(request.overrideReason());
        rec.setDecisionComment(request.comment());
        rec.setDecidedAt(Instant.now());
        rec.setDecidedBy(currentUserId());
        recommendationRepository.save(rec);
        recordDecision(rec, previous, RecommendationStatus.OVERRIDDEN,
                RecommendationDecisionAction.OVERRIDE,
                request.comment(), request.overrideSupplierId(), request.overrideReason());
        auditService.recordTransition("VendorRecommendation", recommendationId, previous.name(),
                "OVERRIDDEN->" + request.overrideSupplierId());
        return toResponse(rec);
    }



    public record DecisionLedgerEntry(
            String decisionId,
            String recommendationId,
            String explanationId,
            String previousStatus,
            String newStatus,
            String decisionAction,
            String recommendedSupplierId,
            String overrideSupplierId,
            String decisionComment,
            String overrideReason,
            String decidedBy,
            java.time.Instant decidedAt
    ) {}

    @Transactional(readOnly = true)
    public List<DecisionLedgerEntry> listDecisions(String recommendationId) {
        require(recommendationId);
        return decisionRepository.findByRecommendationIdOrderByDecidedAtAsc(recommendationId).stream()
                .map(d -> new DecisionLedgerEntry(
                        d.getDecisionId(),
                        d.getRecommendationId(),
                        d.getExplanationId(),
                        d.getPreviousStatus().name(),
                        d.getNewStatus().name(),
                        d.getDecisionAction().name(),
                        d.getRecommendedSupplierId(),
                        d.getOverrideSupplierId(),
                        d.getDecisionComment(),
                        d.getOverrideReason(),
                        d.getDecidedBy(),
                        d.getDecidedAt()))
                .toList();
    }

    private void recordDecision(
            VendorRecommendation rec,
            RecommendationStatus previous,
            RecommendationStatus next,
            RecommendationDecisionAction action,
            String comment,
            String overrideSupplierId,
            String overrideReason) {
        String explanationId = explanationRepository.findByRecommendationId(rec.getRecommendationId())
                .map(e -> e.getExplanationId())
                .orElse(null);
        VendorRecommendationDecision d = new VendorRecommendationDecision();
        d.setDecisionId(UUID.randomUUID().toString());
        d.setRecommendationId(rec.getRecommendationId());
        d.setExplanationId(explanationId);
        d.setPreviousStatus(previous);
        d.setNewStatus(next);
        d.setDecisionAction(action);
        d.setRecommendedSupplierId(rec.getRecommendedSupplierId());
        d.setOverrideSupplierId(overrideSupplierId);
        d.setDecisionComment(comment);
        d.setOverrideReason(overrideReason);
        d.setDecidedBy(currentUserId());
        d.setDecidedAt(Instant.now());
        decisionRepository.save(d);
        auditService.recordCreate("VendorRecommendationDecision", d.getDecisionId(),
                action.name() + " " + previous.name() + "->" + next.name());

        VendorRecommendationDecisionEvent ev = new VendorRecommendationDecisionEvent();
        ev.setEventId(UUID.randomUUID().toString());
        ev.setRecommendationId(rec.getRecommendationId());
        ev.setDecisionId(d.getDecisionId());
        ev.setExplanationId(explanationId);
        ev.setEventType("RECOMMENDATION_" + action.name());
        ev.setPreviousStatus(previous);
        ev.setNewStatus(next);
        ev.setRecommendedSupplierId(rec.getRecommendedSupplierId());
        ev.setOverrideSupplierId(overrideSupplierId);
        ev.setOccurredAt(d.getDecidedAt());
        ev.setOccurredBy(d.getDecidedBy());
        ev.setPublishedFlag(false);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("decisionAction", action.name());
        payload.put("comment", comment);
        payload.put("overrideReason", overrideReason);
        payload.put("humanControlled", true);
        payload.put("autoApproveForbidden", true);
        try {
            ev.setPayloadJson(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            ev.setPayloadJson("{\"humanControlled\":true}");
        }
        decisionEventRepository.save(ev);
        auditService.recordCreate("VendorRecommendationDecisionEvent", ev.getEventId(), ev.getEventType());

        eventPublisher.publishEvent(new VendorRecommendationDecisionEventMessage(
                ev.getEventId(),
                ev.getRecommendationId(),
                ev.getDecisionId(),
                ev.getEventType(),
                previous.name(),
                next.name(),
                rec.getRecommendedSupplierId(),
                overrideSupplierId,
                ev.getOccurredAt(),
                ev.getOccurredBy()));
    }


    public record DecisionEventResponse(
            String eventId,
            String recommendationId,
            String decisionId,
            String explanationId,
            String eventType,
            String previousStatus,
            String newStatus,
            String recommendedSupplierId,
            String overrideSupplierId,
            String payloadJson,
            boolean published,
            Instant publishedAt,
            Instant occurredAt,
            String occurredBy
    ) {}

    @Transactional(readOnly = true)
    public List<DecisionEventResponse> listDecisionEvents(String recommendationId) {
        require(recommendationId);
        return decisionEventRepository.findByRecommendationIdOrderByOccurredAtAsc(recommendationId).stream()
                .map(this::toEvent)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DecisionEventResponse> listUnpublishedEvents() {
        return decisionEventRepository.findByPublishedFlagFalseOrderByOccurredAtAsc().stream()
                .map(this::toEvent)
                .toList();
    }

    @Transactional
    public DecisionEventResponse markEventPublished(String eventId) {
        VendorRecommendationDecisionEvent ev = decisionEventRepository.findById(eventId)
                .orElseThrow(() -> ResourceNotFoundException.of("VendorRecommendationDecisionEvent", eventId));
        ev.setPublishedFlag(true);
        ev.setPublishedAt(Instant.now());
        decisionEventRepository.save(ev);
        auditService.recordUpdate("VendorRecommendationDecisionEvent", eventId, "unpublished", "published");
        return toEvent(ev);
    }

    private DecisionEventResponse toEvent(VendorRecommendationDecisionEvent e) {
        return new DecisionEventResponse(
                e.getEventId(),
                e.getRecommendationId(),
                e.getDecisionId(),
                e.getExplanationId(),
                e.getEventType(),
                e.getPreviousStatus().name(),
                e.getNewStatus().name(),
                e.getRecommendedSupplierId(),
                e.getOverrideSupplierId(),
                e.getPayloadJson(),
                e.isPublishedFlag(),
                e.getPublishedAt(),
                e.getOccurredAt(),
                e.getOccurredBy());
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getExplanation(String recommendationId) {
        require(recommendationId);
        VendorRecommendationExplanation e = explanationRepository.findByRecommendationId(recommendationId)
                .orElseThrow(() -> ResourceNotFoundException.of("VendorRecommendationExplanation", recommendationId));
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("explanationId", e.getExplanationId());
        body.put("recommendationId", e.getRecommendationId());
        body.put("recommendedSupplierId", e.getRecommendedSupplierId());
        body.put("confidenceLevel", e.getConfidenceLevel() == null ? null : e.getConfidenceLevel().name());
        body.put("dataUsedSummary", e.getDataUsedSummary());
        body.put("eligibilitySummary", e.getEligibilitySummary());
        body.put("scoreSummary", e.getScoreSummary());
        body.put("tcoSummary", e.getTcoSummary());
        body.put("strengthsSummary", e.getStrengthsSummary());
        body.put("weaknessesSummary", e.getWeaknessesSummary());
        body.put("riskSummary", e.getRiskSummary());
        body.put("missingDataSummary", e.getMissingDataSummary());
        body.put("costVarianceNotes", e.getCostVarianceNotes());
        body.put("negotiationOpportunities", e.getNegotiationOpportunities());
        body.put("evidenceNotes", e.getEvidenceNotes());
        body.put("decisionBoundaryNote", e.getDecisionBoundaryNote());
        body.put("factJson", e.getFactJson());
        body.put("calculatedJson", e.getCalculatedJson());
        body.put("inferenceJson", e.getInferenceJson());
        body.put("missingJson", e.getMissingJson());
        return body;
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> listByStatus(RecommendationStatus status) {
        if (status == null) {
            throw new BusinessRuleViolationException("status is required", "VS-REC-10");
        }
        return recommendationRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(tenantAccessGuard.requireCustomerId(), status).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecommendationResponse get(String recommendationId) {
        return toResponse(require(recommendationId));
    }

    private VendorRecommendation require(String id) {
        String customerId = tenantAccessGuard.requireCustomerId();
        VendorRecommendation rec = recommendationRepository.findByCustomerIdAndRecommendationId(customerId, id)
                .orElseThrow(() -> ResourceNotFoundException.of("VendorRecommendation", id));
        tenantAccessGuard.assertOwns(rec.getCustomerId(), "VendorRecommendation", id);
        return rec;
    }

    private RecommendationResponse toResponse(VendorRecommendation rec) {
        List<CandidateResponse> ranking = candidateRepository
                .findByRecommendationIdOrderByRankPositionAsc(rec.getRecommendationId())
                .stream()
                .map(c -> new CandidateResponse(
                        c.getCandidateId(), c.getSupplierId(), c.getRankPosition(), c.isEligible(),
                        c.getOverallScore(), c.getTcoTotal(), c.getTcoCurrency(), c.getTcoDataComplete(),
                        c.getFailedGates(), c.getStrengths(), c.getWeaknesses(),
                        c.getLifecycleStatus(), c.isLifecycleBlocked()))
                .toList();
        return new RecommendationResponse(
                rec.getRecommendationId(), rec.getSourcingEventCode(), rec.getBomNodeId(),
                rec.getProgramId(), rec.getTemplateId(), rec.getStatus().name(),
                rec.getRecommendedSupplierId(), rec.getRationale(),
                rec.getConfidenceLevel() == null ? null : rec.getConfidenceLevel().name(),
                rec.getMissingDataSummary(), ranking,
                rec.getSubmittedAt(), rec.getDecidedAt(),
                rec.getDecisionComment(), rec.getOverrideReason());
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.automotive.costing.platform.user.entity.User u) {
            return u.getUserId();
        }
        return null;
    }
}
