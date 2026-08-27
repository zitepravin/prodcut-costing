package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.vendorselection.entity.QuestionAnswerType;
import com.automotive.costing.vendorselection.entity.VendorQuestion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Evaluates mandatory gate and simple scoring for a single questionnaire answer.
 * Historical responses bind to question_id/version — editing a question creates a new version.
 */
@Component
public class QuestionnaireGateEngine {

    public record GateScoreResult(Boolean gatePassed, BigDecimal scoredValue, String detail) {}

    public GateScoreResult evaluate(VendorQuestion question, Boolean answerBoolean,
                                    BigDecimal answerNumber, String answerText) {
        Boolean gate = null;
        BigDecimal score = null;
        String detail = "Recorded";

        if (question.isMandatoryGate()) {
            if (question.getAnswerType() == QuestionAnswerType.YES_NO) {
                gate = Boolean.TRUE.equals(answerBoolean);
                detail = gate ? "Mandatory YES satisfied" : "Mandatory gate FAILED (expected YES)";
            } else if (answerNumber != null) {
                gate = answerNumber.compareTo(BigDecimal.ZERO) > 0;
                detail = gate ? "Mandatory numeric response present" : "Mandatory gate FAILED (non-positive)";
            } else if (answerText != null && !answerText.isBlank()) {
                gate = true;
                detail = "Mandatory text present";
            } else {
                gate = false;
                detail = "Mandatory gate FAILED (no answer)";
            }
        }

        if (question.getAnswerType() == QuestionAnswerType.RATING && answerNumber != null) {
            score = answerNumber;
        } else if (question.getAnswerType() == QuestionAnswerType.YES_NO && answerBoolean != null) {
            score = Boolean.TRUE.equals(answerBoolean) ? new BigDecimal("100") : BigDecimal.ZERO;
        } else if (question.getWeight() != null && answerNumber != null) {
            score = answerNumber;
        }

        return new GateScoreResult(gate, score, detail);
    }
}
