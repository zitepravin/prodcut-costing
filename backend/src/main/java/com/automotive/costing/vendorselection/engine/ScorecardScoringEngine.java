package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Configurable weighted scorecard (B4). Weights must total 100%.
 * Category raw scores expected on 0–100 scale; overall is weighted sum.
 * No hard-coded industry weights — caller supplies template weights.
 */
@Component
public class ScorecardScoringEngine {

    private static final BigDecimal HUNDRED = new BigDecimal("100.0000");
    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.01");
    private static final int SCALE = 4;

    public record CategoryWeight(String categoryCode, String categoryName, BigDecimal weightPercent) {}

    public record CategoryScoreInput(String categoryCode, BigDecimal rawScore0to100) {}

    public record CategoryScoreLine(
            String categoryCode,
            String categoryName,
            BigDecimal weightPercent,
            BigDecimal rawScore,
            BigDecimal weightedScore
    ) {}

    public record ScoreOutput(
            BigDecimal overallScore,
            BigDecimal maxScore,
            List<CategoryScoreLine> lines
    ) {}

    public void assertWeightsTotal100(List<CategoryWeight> weights) {
        if (weights == null || weights.isEmpty()) {
            throw new BusinessRuleViolationException("Scorecard must have at least one category", "VS-SC-01");
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (CategoryWeight w : weights) {
            if (w.weightPercent() == null || w.weightPercent().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleViolationException(
                        "Category weight must be positive: " + w.categoryCode(), "VS-SC-02");
            }
            sum = sum.add(w.weightPercent());
        }
        if (sum.subtract(HUNDRED).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new BusinessRuleViolationException(
                    "Category weights must total 100% (was " + sum + ")", "VS-SC-03");
        }
    }

    public ScoreOutput score(List<CategoryWeight> weights, List<CategoryScoreInput> scores) {
        assertWeightsTotal100(weights);
        Map<String, BigDecimal> scoreMap = new java.util.HashMap<>();
        if (scores != null) {
            for (CategoryScoreInput s : scores) {
                if (s.rawScore0to100() == null) {
                    throw new BusinessRuleViolationException(
                            "Raw score required for category " + s.categoryCode(), "VS-SC-04");
                }
                if (s.rawScore0to100().compareTo(BigDecimal.ZERO) < 0
                        || s.rawScore0to100().compareTo(HUNDRED) > 0) {
                    throw new BusinessRuleViolationException(
                            "Raw score must be 0–100 for " + s.categoryCode(), "VS-SC-05");
                }
                scoreMap.put(s.categoryCode(), s.rawScore0to100());
            }
        }
        List<CategoryScoreLine> lines = new ArrayList<>();
        BigDecimal overall = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        for (CategoryWeight w : weights) {
            BigDecimal raw = scoreMap.get(w.categoryCode());
            if (raw == null) {
                throw new BusinessRuleViolationException(
                        "Missing score for category " + w.categoryCode(), "VS-SC-06");
            }
            BigDecimal weighted = raw.multiply(w.weightPercent())
                    .divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
            lines.add(new CategoryScoreLine(w.categoryCode(), w.categoryName(),
                    w.weightPercent().setScale(SCALE, RoundingMode.HALF_UP),
                    raw.setScale(SCALE, RoundingMode.HALF_UP),
                    weighted));
            overall = overall.add(weighted);
        }
        return new ScoreOutput(overall.setScale(SCALE, RoundingMode.HALF_UP), HUNDRED, lines);
    }
}
