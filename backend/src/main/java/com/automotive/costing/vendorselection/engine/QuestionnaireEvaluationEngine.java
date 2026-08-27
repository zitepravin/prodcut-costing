package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import com.automotive.costing.vendorselection.entity.VendorQuestion;
import com.automotive.costing.vendorselection.entity.VendorQuestionnaireResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates active questionnaire bank vs latest supplier answers.
 * Pure logic — no I/O. Does not invent scores when answers are missing.
 * Evidence required: PENDING/FAILED/EXPIRED does not satisfy mandatory gates (V51).
 */
@Component
public class QuestionnaireEvaluationEngine {

    public record QuestionLine(
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

    public record EvaluationOutput(
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
            List<QuestionLine> lines
    ) {}

    public EvaluationOutput evaluate(List<VendorQuestion> activeQuestions, List<VendorQuestionnaireResponse> responses) {
        Map<String, VendorQuestionnaireResponse> latestByCode = new HashMap<>();
        if (responses != null) {
            for (VendorQuestionnaireResponse r : responses) {
                if (r == null || r.getQuestionCode() == null) continue;
                VendorQuestionnaireResponse existing = latestByCode.get(r.getQuestionCode());
                if (existing == null || (r.getRespondedAt() != null && existing.getRespondedAt() != null
                        && r.getRespondedAt().isAfter(existing.getRespondedAt()))) {
                    latestByCode.put(r.getQuestionCode(), r);
                } else if (existing == null) {
                    latestByCode.put(r.getQuestionCode(), r);
                }
            }
        }

        List<QuestionLine> lines = new ArrayList<>();
        List<String> failedGates = new ArrayList<>();
        List<String> missingRequired = new ArrayList<>();
        int answered = 0;
        int mandatoryCount = 0;
        int mandatoryFailed = 0;
        int evidencePending = 0;
        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal maxPossible = BigDecimal.ZERO;

        List<VendorQuestion> questions = activeQuestions == null ? List.of() : activeQuestions;

        for (VendorQuestion q : questions) {
            VendorQuestionnaireResponse r = latestByCode.get(q.getQuestionCode());
            boolean isAnswered = r != null;
            if (isAnswered) {
                answered++;
            }

            Boolean gatePassed = null;
            BigDecimal scored = null;
            EvidenceStatus evidenceStatus = null;
            String detail;

            if (q.isMandatoryGate()) {
                mandatoryCount++;
            }

            if (isAnswered) {
                gatePassed = r.getGatePassed();
                scored = r.getScoredValue();
                evidenceStatus = r.getEvidenceStatus();
                if (evidenceStatus == EvidenceStatus.PENDING) {
                    evidencePending++;
                }

                boolean evidenceOk = true;
                if (q.isEvidenceRequired()) {
                    evidenceOk = evidenceStatus == EvidenceStatus.VERIFIED
                            || evidenceStatus == EvidenceStatus.WAIVED
                            || evidenceStatus == EvidenceStatus.NOT_REQUIRED;
                }

                if (q.isMandatoryGate()) {
                    boolean answerGateOk = gatePassed != null && gatePassed;
                    if (!answerGateOk || !evidenceOk) {
                        mandatoryFailed++;
                        failedGates.add(q.getQuestionCode());
                        if (!answerGateOk) {
                            detail = "Mandatory gate FAILED (answer)";
                        } else {
                            detail = "Mandatory gate FAILED (evidence " + evidenceStatus + " — need VERIFIED or WAIVED)";
                        }
                        gatePassed = false;
                    } else {
                        detail = "Mandatory gate PASSED";
                    }
                } else if (q.isEvidenceRequired() && !evidenceOk) {
                    detail = "Answered but evidence not satisfied (" + evidenceStatus + ")";
                } else {
                    detail = "Answered";
                }

                if (scored != null && q.getWeight() != null) {
                    weighted = weighted.add(scored.multiply(q.getWeight()));
                }
            } else {
                if (q.isRequiredFlag()) {
                    missingRequired.add(q.getQuestionCode());
                    detail = "Required question not answered";
                } else if (q.isMandatoryGate()) {
                    mandatoryFailed++;
                    failedGates.add(q.getQuestionCode());
                    detail = "Mandatory gate FAILED (no answer)";
                } else {
                    detail = "Optional question not answered";
                }
            }

            if (q.getWeight() != null) {
                maxPossible = maxPossible.add(q.getWeight());
            }

            lines.add(new QuestionLine(
                    q.getQuestionCode(),
                    q.getVersionNo(),
                    q.getCategory() != null ? q.getCategory().name() : null,
                    q.isRequiredFlag(),
                    q.isMandatoryGate(),
                    isAnswered,
                    gatePassed,
                    scored,
                    q.getWeight(),
                    evidenceStatus,
                    detail
            ));
        }

        boolean requiredAnswered = missingRequired.isEmpty();
        boolean gatesPassed = mandatoryFailed == 0;
        boolean overall = requiredAnswered && gatesPassed;

        BigDecimal weightedScore = maxPossible.compareTo(BigDecimal.ZERO) > 0
                ? weighted.setScale(4, RoundingMode.HALF_UP)
                : null;
        BigDecimal maxScore = maxPossible.compareTo(BigDecimal.ZERO) > 0
                ? maxPossible.setScale(4, RoundingMode.HALF_UP)
                : null;

        return new EvaluationOutput(
                overall,
                gatesPassed,
                requiredAnswered,
                questions.size(),
                answered,
                mandatoryCount,
                mandatoryFailed,
                evidencePending,
                weightedScore,
                maxScore,
                failedGates,
                missingRequired,
                lines
        );
    }
}
