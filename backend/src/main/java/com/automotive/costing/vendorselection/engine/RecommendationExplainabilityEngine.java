package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.vendorselection.entity.ConfidenceLevel;
import com.automotive.costing.vendorselection.engine.RecommendationRankingEngine.RankedCandidate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds structured explainability payload for a recommendation draft.
 * Classifies content as FACT / CALCULATED / INFERENCE / MISSING — never invents supplier data.
 */
@Component
public class RecommendationExplainabilityEngine {

    public static final String DECISION_BOUNDARY =
            "This is decision support only. Human approve / reject / override is mandatory. "
                    + "The system never awards business automatically.";

    public record ExplanationInput(
            String recommendedSupplierId,
            ConfidenceLevel confidenceLevel,
            String rankingRationale,
            String missingDataFromRanking,
            List<RankedCandidate> ranked
    ) {}

    public record ExplanationOutput(
            String dataUsedSummary,
            String eligibilitySummary,
            String scoreSummary,
            String tcoSummary,
            String strengthsSummary,
            String weaknessesSummary,
            String riskSummary,
            String missingDataSummary,
            String costVarianceNotes,
            String negotiationOpportunities,
            String evidenceNotes,
            String decisionBoundaryNote,
            Map<String, Object> facts,
            Map<String, Object> calculated,
            Map<String, Object> inferences,
            Map<String, Object> missing
    ) {}

    public ExplanationOutput build(ExplanationInput input) {
        List<RankedCandidate> ranked = input.ranked() == null ? List.of() : input.ranked();

        long eligibleCount = ranked.stream().filter(RankedCandidate::eligible).count();
        long ineligibleCount = ranked.size() - eligibleCount;
        long incompleteTco = ranked.stream()
                .filter(c -> !c.tcoDataComplete())
                .count();
        long missingScore = ranked.stream().filter(c -> c.overallScore() == null).count();

        List<String> dataUsed = new ArrayList<>();
        dataUsed.add("Candidates evaluated: " + ranked.size());
        dataUsed.add("Eligible: " + eligibleCount + ", Ineligible: " + ineligibleCount);
        if (input.recommendedSupplierId() != null) {
            dataUsed.add("Draft top-ranked supplier: " + input.recommendedSupplierId());
        }

        String eligibilitySummary = ranked.stream()
                .map(c -> c.supplierId() + "=" + (c.eligible() ? "ELIGIBLE" : "NOT_ELIGIBLE"
                        + (c.failedGates() == null || c.failedGates().isEmpty() ? ""
                        : " gates:" + String.join(",", c.failedGates()))))
                .collect(Collectors.joining("; "));

        String scoreSummary = ranked.stream()
                .map(c -> c.supplierId() + " score="
                        + (c.overallScore() != null ? c.overallScore() : "DATA_NOT_AVAILABLE")
                        + " rank=" + c.rankPosition())
                .collect(Collectors.joining("; "));

        String tcoSummary = ranked.stream()
                .map(c -> c.supplierId() + " TCO="
                        + (c.tcoTotal() != null ? c.tcoTotal().toPlainString() : "DATA_NOT_AVAILABLE")
                        + (c.tcoDataComplete() ? " complete" : " incomplete"))
                .collect(Collectors.joining("; "));

        String strengths = ranked.stream()
                .filter(c -> c.strengths() != null && !c.strengths().isBlank())
                .map(c -> c.supplierId() + ": " + c.strengths())
                .collect(Collectors.joining(" | "));

        String weaknesses = ranked.stream()
                .filter(c -> c.weaknesses() != null && !c.weaknesses().isBlank())
                .map(c -> c.supplierId() + ": " + c.weaknesses())
                .collect(Collectors.joining(" | "));

        // Risk: only from failed gates / ineligibility — do not invent risk register content
        String riskSummary = ranked.stream()
                .filter(c -> !c.eligible())
                .map(c -> c.supplierId() + " ineligible"
                        + (c.failedGates() != null && !c.failedGates().isEmpty()
                        ? " (" + String.join(",", c.failedGates()) + ")" : ""))
                .collect(Collectors.joining("; "));
        if (riskSummary.isBlank()) {
            riskSummary = "No ineligibility risk flags in ranking inputs";
        }

        List<String> missingParts = new ArrayList<>();
        if (input.missingDataFromRanking() != null && !input.missingDataFromRanking().isBlank()) {
            missingParts.add(input.missingDataFromRanking());
        }
        if (incompleteTco > 0) {
            missingParts.add("TCO incomplete for " + incompleteTco + " candidate(s)");
        }
        if (missingScore > 0) {
            missingParts.add("Score missing for " + missingScore + " candidate(s)");
        }
        String missingSummary = missingParts.isEmpty() ? "No missing-data flags reported" : String.join("; ", missingParts);

        // Cost variance: only note when both score and TCO present for top vs others — no fabricated variance amounts
        String costVarianceNotes = "Cost variance vs should-cost is not calculated in this snapshot unless provided by upstream costing modules.";

        List<String> negotiation = new ArrayList<>();
        var eligibleWithTco = ranked.stream()
                .filter(RankedCandidate::eligible)
                .filter(c -> c.tcoTotal() != null)
                .toList();
        if (eligibleWithTco.size() >= 2) {
            var minTco = eligibleWithTco.stream().map(RankedCandidate::tcoTotal).min(java.math.BigDecimal::compareTo).orElse(null);
            var maxTco = eligibleWithTco.stream().map(RankedCandidate::tcoTotal).max(java.math.BigDecimal::compareTo).orElse(null);
            if (minTco != null && maxTco != null && maxTco.compareTo(minTco) > 0) {
                negotiation.add("CALCULATED: Eligible TCO spread exists (min=" + minTco.toPlainString()
                        + ", max=" + maxTco.toPlainString() + "). Consider volume, packaging, or logistics negotiation on higher-TCO candidates.");
            }
        } else {
            negotiation.add("MISSING DATA: Insufficient eligible TCO pairs to derive negotiation spread.");
        }
        String negotiationOpportunities = String.join(" ", negotiation);

        String evidenceNotes = "Evidence status is not re-verified in ranking. Use Supplier 360 / policy / questionnaire evaluation for evidence detail.";

        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("candidateCount", ranked.size());
        facts.put("eligibleCount", eligibleCount);
        facts.put("ineligibleCount", ineligibleCount);
        facts.put("recommendedSupplierId", input.recommendedSupplierId());
        facts.put("classification", "FACT");

        Map<String, Object> calculated = new LinkedHashMap<>();
        calculated.put("rankOrder", ranked.stream().map(RankedCandidate::supplierId).toList());
        calculated.put("confidenceLevel", input.confidenceLevel() != null ? input.confidenceLevel().name() : null);
        calculated.put("classification", "CALCULATED RESULT");

        Map<String, Object> inferences = new LinkedHashMap<>();
        inferences.put("rankingRationale", input.rankingRationale());
        inferences.put("note", "Ranking preference among eligible candidates is an inference from provided scores/TCO — not a verified business award.");
        inferences.put("classification", "AI INFERENCE");

        Map<String, Object> missing = new LinkedHashMap<>();
        missing.put("summary", missingSummary);
        missing.put("incompleteTcoCount", incompleteTco);
        missing.put("missingScoreCount", missingScore);
        missing.put("classification", "MISSING DATA");

        return new ExplanationOutput(
                String.join(". ", dataUsed),
                eligibilitySummary.isBlank() ? "No eligibility data" : eligibilitySummary,
                scoreSummary.isBlank() ? "No scores" : scoreSummary,
                tcoSummary.isBlank() ? "No TCO data" : tcoSummary,
                strengths.isBlank() ? "None recorded" : strengths,
                weaknesses.isBlank() ? "None recorded" : weaknesses,
                riskSummary,
                missingSummary,
                costVarianceNotes,
                negotiationOpportunities,
                evidenceNotes,
                DECISION_BOUNDARY,
                facts,
                calculated,
                inferences,
                missing
        );
    }

}
