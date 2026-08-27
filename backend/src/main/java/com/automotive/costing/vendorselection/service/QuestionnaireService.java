package com.automotive.costing.vendorselection.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.engine.QuestionnaireGateEngine;
import com.automotive.costing.vendorselection.engine.QuestionnaireEvaluationEngine;
import com.automotive.costing.vendorselection.repository.VendorQuestionnaireEvaluationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.automotive.costing.vendorselection.entity.*;
import com.automotive.costing.vendorselection.repository.VendorQuestionRepository;
import com.automotive.costing.vendorselection.repository.VendorQuestionnaireResponseRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionnaireService {

    private final VendorQuestionRepository questionRepository;
    private final VendorQuestionnaireResponseRepository responseRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final SupplierMasterService supplierMasterService;
    private final QuestionnaireGateEngine gateEngine;
    private final QuestionnaireEvaluationEngine evaluationEngine;
    private final VendorQuestionnaireEvaluationRepository evaluationRepository;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public record CreateQuestionRequest(
            String questionCode,
            QuestionCategory category,
            String questionText,
            QuestionAnswerType answerType,
            boolean requiredFlag,
            boolean evidenceRequired,
            boolean mandatoryGate,
            BigDecimal weight,
            String scoringRule,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {}

    public record QuestionResponse(
            String questionId,
            String questionCode,
            int versionNo,
            QuestionCategory category,
            String questionText,
            QuestionAnswerType answerType,
            boolean requiredFlag,
            boolean evidenceRequired,
            boolean mandatoryGate,
            BigDecimal weight,
            String scoringRule,
            boolean activeFlag
    ) {}

    public record SubmitAnswerRequest(
            String supplierId,
            String questionId,
            String answerText,
            BigDecimal answerNumber,
            Boolean answerBoolean,
            LocalDate answerDate,
            String evidenceRef
    ) {}

    public record VerifyEvidenceRequest(
            EvidenceStatus evidenceStatus,
            String evidenceRef,
            String waiverReason
    ) {}

    public record AnswerResponse(
            String responseId,
            String supplierId,
            String questionId,
            String questionCode,
            int questionVersion,
            String answerText,
            BigDecimal answerNumber,
            Boolean answerBoolean,
            LocalDate answerDate,
            EvidenceStatus evidenceStatus,
            Boolean gatePassed,
            BigDecimal scoredValue,
            Instant respondedAt,
            String verifiedBy,
            Instant verifiedAt,
            String waiverReason,
            String evidenceRef
    ) {}

    @Transactional
    public QuestionResponse createQuestion(CreateQuestionRequest request) {
        if (request.questionCode() == null || request.questionCode().isBlank()) {
            throw new BusinessRuleViolationException("questionCode is required", "VS-Q-01");
        }
        if (request.questionText() == null || request.questionText().isBlank()) {
            throw new BusinessRuleViolationException("questionText is required", "VS-Q-02");
        }
        if (request.answerType() == null || request.category() == null) {
            throw new BusinessRuleViolationException("category and answerType are required", "VS-Q-03");
        }
        String code = request.questionCode().trim().toUpperCase();
        int nextVersion = questionRepository.findByCustomerIdAndQuestionCodeOrderByVersionNoDesc(tenantAccessGuard.requireCustomerId(), code).stream()
                .mapToInt(VendorQuestion::getVersionNo)
                .max()
                .orElse(0) + 1;

        // Prior active versions stay in DB for historical responses; new version is active
        questionRepository.findByCustomerIdAndQuestionCodeOrderByVersionNoDesc(tenantAccessGuard.requireCustomerId(), code).forEach(q -> {
            if (q.isActiveFlag()) {
                q.setActiveFlag(false);
                questionRepository.save(q);
            }
        });

        VendorQuestion q = new VendorQuestion();
        q.setCustomerId(tenantAccessGuard.requireCustomerId());
        q.setQuestionId(UUID.randomUUID().toString());
        q.setQuestionCode(code);
        q.setVersionNo(nextVersion);
        q.setCategory(request.category());
        q.setQuestionText(request.questionText().trim());
        q.setAnswerType(request.answerType());
        q.setRequiredFlag(request.requiredFlag());
        q.setEvidenceRequired(request.evidenceRequired());
        q.setMandatoryGate(request.mandatoryGate());
        q.setWeight(request.weight());
        q.setScoringRule(request.scoringRule());
        q.setActiveFlag(true);
        q.setEffectiveFrom(request.effectiveFrom());
        q.setEffectiveTo(request.effectiveTo());
        questionRepository.save(q);
        auditService.recordCreate("VendorQuestion", q.getQuestionId(), code + " v" + nextVersion);
        return toQuestion(q);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> listActiveQuestions() {
        return listActiveQuestions(null);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> listActiveQuestions(QuestionCategory category) {
        var stream = category == null
                ? questionRepository.findByCustomerIdAndActiveFlagTrueOrderByCategoryAscQuestionCodeAsc(tenantAccessGuard.requireCustomerId()).stream()
                : questionRepository.findByCustomerIdAndCategoryAndActiveFlagTrueOrderByQuestionCodeAsc(tenantAccessGuard.requireCustomerId(), category).stream();
        return stream.map(this::toQuestion).toList();
    }

    @Transactional
    public AnswerResponse submitAnswer(SubmitAnswerRequest request) {
        supplierMasterService.require(request.supplierId());
        VendorQuestion question = questionRepository.findByCustomerIdAndQuestionId(tenantAccessGuard.requireCustomerId(), request.questionId())
                .orElseThrow(() -> ResourceNotFoundException.of("VendorQuestion", request.questionId()));

        if (question.isRequiredFlag()
                && isEmptyAnswer(request.answerText(), request.answerNumber(), request.answerBoolean(), request.answerDate())) {
            throw new BusinessRuleViolationException("Answer required for question " + question.getQuestionCode(), "VS-Q-04");
        }
        if (question.isEvidenceRequired() && (request.evidenceRef() == null || request.evidenceRef().isBlank())) {
            throw new BusinessRuleViolationException(
                    "Evidence reference required for question " + question.getQuestionCode(), "VS-Q-05");
        }

        var gate = gateEngine.evaluate(question, request.answerBoolean(), request.answerNumber(), request.answerText());

        VendorQuestionnaireResponse r = new VendorQuestionnaireResponse();
        r.setCustomerId(tenantAccessGuard.requireCustomerId());
        r.setResponseId(UUID.randomUUID().toString());
        r.setSupplierId(request.supplierId());
        r.setQuestionId(question.getQuestionId());
        r.setQuestionCode(question.getQuestionCode());
        r.setQuestionVersion(question.getVersionNo());
        r.setAnswerText(request.answerText());
        r.setAnswerNumber(request.answerNumber());
        r.setAnswerBoolean(request.answerBoolean());
        r.setAnswerDate(request.answerDate());
        r.setEvidenceStatus(question.isEvidenceRequired() ? EvidenceStatus.PENDING : EvidenceStatus.NOT_REQUIRED);
        r.setEvidenceRef(request.evidenceRef());
        r.setGatePassed(gate.gatePassed());
        r.setScoredValue(gate.scoredValue());
        r.setRespondedAt(Instant.now());
        r.setRespondedBy(currentUserId());
        responseRepository.save(r);
        auditService.recordCreate("VendorQuestionnaireResponse", r.getResponseId(),
                question.getQuestionCode() + " gate=" + gate.gatePassed());
        return toAnswer(r);
    }


    @Transactional
    public AnswerResponse verifyEvidence(String responseId, VerifyEvidenceRequest req) {
        if (req == null || req.evidenceStatus() == null) {
            throw new BusinessRuleViolationException("evidenceStatus is required", "VS-Q-10");
        }
        VendorQuestionnaireResponse r = responseRepository.findByCustomerIdAndResponseId(tenantAccessGuard.requireCustomerId(), responseId)
                .orElseThrow(() -> ResourceNotFoundException.of("VendorQuestionnaireResponse", responseId));
        EvidenceStatus status = req.evidenceStatus();
        if (status == EvidenceStatus.WAIVED) {
            if (req.waiverReason() == null || req.waiverReason().isBlank()) {
                throw new BusinessRuleViolationException(
                        "WAIVED status requires waiverReason for audit", "VS-Q-11");
            }
            r.setWaiverReason(req.waiverReason().trim());
        }
        if (status == EvidenceStatus.VERIFIED) {
            String ref = req.evidenceRef() != null ? req.evidenceRef() : r.getEvidenceRef();
            if (ref == null || ref.isBlank()) {
                throw new BusinessRuleViolationException(
                        "VERIFIED requires evidenceRef on the response", "VS-Q-12");
            }
            r.setEvidenceRef(ref);
        }
        if (req.evidenceRef() != null && !req.evidenceRef().isBlank()) {
            r.setEvidenceRef(req.evidenceRef().trim());
        }
        r.setEvidenceStatus(status);
        r.setVerifiedBy(currentUserId());
        r.setVerifiedAt(Instant.now());
        responseRepository.save(r);
        auditService.recordUpdate("VendorQuestionnaireResponse", responseId, null, status.name());
        return toAnswer(r);
    }

    @Transactional(readOnly = true)
    public List<AnswerResponse> listResponses(String supplierId) {
        return responseRepository.findByCustomerIdAndSupplierIdOrderByCreatedAtDesc(tenantAccessGuard.requireCustomerId(), supplierId).stream()
                .map(this::toAnswer)
                .toList();
    }

    private static boolean isEmptyAnswer(String text, BigDecimal number, Boolean bool, LocalDate date) {
        return (text == null || text.isBlank()) && number == null && bool == null && date == null;
    }

    private QuestionResponse toQuestion(VendorQuestion q) {
        return new QuestionResponse(q.getQuestionId(), q.getQuestionCode(), q.getVersionNo(),
                q.getCategory(), q.getQuestionText(), q.getAnswerType(), q.isRequiredFlag(),
                q.isEvidenceRequired(), q.isMandatoryGate(), q.getWeight(), q.getScoringRule(),
                q.isActiveFlag());
    }

    private AnswerResponse toAnswer(VendorQuestionnaireResponse r) {
        return new AnswerResponse(
                r.getResponseId(),
                r.getSupplierId(),
                r.getQuestionId(),
                r.getQuestionCode(),
                r.getQuestionVersion(),
                r.getAnswerText(),
                r.getAnswerNumber(),
                r.getAnswerBoolean(),
                r.getAnswerDate(),
                r.getEvidenceStatus(),
                r.getGatePassed(),
                r.getScoredValue(),
                r.getRespondedAt(),
                r.getVerifiedBy(),
                r.getVerifiedAt(),
                r.getWaiverReason(),
                r.getEvidenceRef());
    }


    public record EvaluationLineResponse(
            String questionCode,
            int questionVersion,
            String category,
            boolean required,
            boolean mandatoryGate,
            boolean answered,
            Boolean gatePassed,
            BigDecimal scoredValue,
            BigDecimal weight,
            EvidenceStatus evidenceStatus,
            String detail
    ) {}

    public record EvaluationResponse(
            String evaluationId,
            String supplierId,
            boolean overallEligible,
            boolean mandatoryGatesPassed,
            boolean requiredAnswered,
            int totalActiveQuestions,
            int answeredCount,
            int mandatoryGateCount,
            int mandatoryGateFailed,
            int evidencePendingCount,
            BigDecimal weightedScore,
            BigDecimal maxPossibleScore,
            List<String> failedGateCodes,
            List<String> missingQuestionCodes,
            List<EvaluationLineResponse> lines,
            Instant evaluatedAt
    ) {}

    @Transactional
    public EvaluationResponse evaluateSupplier(String supplierId, String notes) {
        supplierMasterService.require(supplierId);
        List<VendorQuestion> active = questionRepository.findByCustomerIdAndActiveFlagTrueOrderByCategoryAscQuestionCodeAsc(tenantAccessGuard.requireCustomerId());
        List<VendorQuestionnaireResponse> responses =
                responseRepository.findByCustomerIdAndSupplierIdOrderByCreatedAtDesc(tenantAccessGuard.requireCustomerId(), supplierId);

        var output = evaluationEngine.evaluate(active, responses);

        VendorQuestionnaireEvaluation entity = new VendorQuestionnaireEvaluation();
        entity.setCustomerId(tenantAccessGuard.requireCustomerId());
        entity.setEvaluationId(UUID.randomUUID().toString());
        entity.setSupplierId(supplierId);
        entity.setOverallEligible(output.overallEligible());
        entity.setMandatoryGatesPassed(output.mandatoryGatesPassed());
        entity.setRequiredAnswered(output.requiredAnswered());
        entity.setTotalActiveQuestions(output.totalActiveQuestions());
        entity.setAnsweredCount(output.answeredCount());
        entity.setMandatoryGateCount(output.mandatoryGateCount());
        entity.setMandatoryGateFailed(output.mandatoryGateFailed());
        entity.setEvidencePendingCount(output.evidencePendingCount());
        entity.setWeightedScore(output.weightedScore());
        entity.setMaxPossibleScore(output.maxPossibleScore());
        entity.setFailedGateCodes(String.join(",", output.failedGateCodes()));
        entity.setMissingQuestionCodes(String.join(",", output.missingQuestionCodes()));
        try {
            entity.setEvaluationJson(objectMapper.writeValueAsString(output.lines()));
        } catch (JsonProcessingException e) {
            entity.setEvaluationJson("[]");
        }
        entity.setEvaluatedAt(Instant.now());
        entity.setEvaluatedBy(currentUserId());
        entity.setNotes(notes);
        evaluationRepository.save(entity);
        auditService.recordCreate("VendorQuestionnaireEvaluation", entity.getEvaluationId(),
                supplierId + " eligible=" + output.overallEligible());

        return toEvaluation(entity, output.lines());
    }

    @Transactional(readOnly = true)
    public EvaluationResponse latestEvaluation(String supplierId) {
        supplierMasterService.require(supplierId);
        VendorQuestionnaireEvaluation entity = evaluationRepository
                .findByCustomerIdAndSupplierIdOrderByEvaluatedAtDesc(tenantAccessGuard.requireCustomerId(), supplierId).stream().findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("VendorQuestionnaireEvaluation", supplierId));
        return toEvaluation(entity, null);
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> evaluationHistory(String supplierId) {
        supplierMasterService.require(supplierId);
        return evaluationRepository.findByCustomerIdAndSupplierIdOrderByEvaluatedAtDesc(tenantAccessGuard.requireCustomerId(), supplierId).stream()
                .map(e -> toEvaluation(e, null))
                .toList();
    }

    private EvaluationResponse toEvaluation(VendorQuestionnaireEvaluation e,
                                            List<QuestionnaireEvaluationEngine.QuestionLine> lines) {
        List<EvaluationLineResponse> lineResponses = List.of();
        if (lines != null) {
            lineResponses = lines.stream()
                    .map(l -> new EvaluationLineResponse(
                            l.questionCode(), l.questionVersion(), l.category(),
                            l.required(), l.mandatoryGate(), l.answered(),
                            l.gatePassed(), l.scoredValue(), l.weight(),
                            l.evidenceStatus(), l.detail()))
                    .toList();
        }
        List<String> failed = e.getFailedGateCodes() == null || e.getFailedGateCodes().isBlank()
                ? List.of() : List.of(e.getFailedGateCodes().split(","));
        List<String> missing = e.getMissingQuestionCodes() == null || e.getMissingQuestionCodes().isBlank()
                ? List.of() : List.of(e.getMissingQuestionCodes().split(","));
        return new EvaluationResponse(
                e.getEvaluationId(),
                e.getSupplierId(),
                e.isOverallEligible(),
                e.isMandatoryGatesPassed(),
                e.isRequiredAnswered(),
                e.getTotalActiveQuestions(),
                e.getAnsweredCount(),
                e.getMandatoryGateCount(),
                e.getMandatoryGateFailed(),
                e.getEvidencePendingCount(),
                e.getWeightedScore(),
                e.getMaxPossibleScore(),
                failed.stream().filter(s -> !s.isBlank()).toList(),
                missing.stream().filter(s -> !s.isBlank()).toList(),
                lineResponses,
                e.getEvaluatedAt()
        );
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.automotive.costing.platform.user.entity.User u) {
            return u.getUserId();
        }
        return null;
    }
}
