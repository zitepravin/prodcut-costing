package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.vendorselection.entity.ConfidenceLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Evidence-backed ranking for recommendation drafts (B5).
 * Eligible suppliers ranked by overall score DESC, then TCO ASC when both present.
 * BLOCKED / SUSPENDED lifecycle status forces ineligible (V58) — never auto-recommended.
 * Does not approve suppliers — only produces a ranked draft.
 */
@Component
public class RecommendationRankingEngine {

    public record CandidateInput(
            String supplierId,
            boolean eligible,
            BigDecimal overallScore,
            BigDecimal tcoTotal,
            boolean tcoDataComplete,
            List<String> failedGates,
            List<String> missingDataHints,
            String lifecycleStatus
    ) {}

    public record RankedCandidate(
            String supplierId,
            int rankPosition,
            boolean eligible,
            BigDecimal overallScore,
            BigDecimal tcoTotal,
            boolean tcoDataComplete,
            List<String> failedGates,
            String strengths,
            String weaknesses,
            String lifecycleStatus,
            boolean lifecycleBlocked
    ) {}

    public record RankingOutput(
            String recommendedSupplierId,
            ConfidenceLevel confidenceLevel,
            String rationale,
            String missingDataSummary,
            List<RankedCandidate> ranking
    ) {}

    public RankingOutput rank(List<CandidateInput> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return new RankingOutput(null, ConfidenceLevel.LOW,
                    "No candidates supplied.", "No supplier candidates", List.of());
        }

        List<CandidateInput> normalized = candidates.stream().map(this::applyLifecycleHardStop).toList();

        List<CandidateInput> eligible = normalized.stream().filter(CandidateInput::eligible).toList();
        List<CandidateInput> ineligible = normalized.stream().filter(c -> !c.eligible()).toList();

        Comparator<CandidateInput> byScoreThenTco = Comparator
                .comparing((CandidateInput c) -> c.overallScore() == null ? BigDecimal.ZERO : c.overallScore(),
                        Comparator.reverseOrder())
                .thenComparing(c -> c.tcoTotal() == null ? new BigDecimal("999999999") : c.tcoTotal());

        List<CandidateInput> ordered = new ArrayList<>();
        ordered.addAll(eligible.stream().sorted(byScoreThenTco).toList());
        ordered.addAll(ineligible.stream().sorted(byScoreThenTco).toList());

        List<RankedCandidate> ranked = new ArrayList<>();
        int pos = 1;
        for (CandidateInput c : ordered) {
            boolean blocked = isLifecycleBlocked(c.lifecycleStatus());
            ranked.add(new RankedCandidate(
                    c.supplierId(),
                    pos++,
                    c.eligible(),
                    c.overallScore(),
                    c.tcoTotal(),
                    c.tcoDataComplete(),
                    c.failedGates(),
                    buildStrengths(c),
                    buildWeaknesses(c),
                    c.lifecycleStatus(),
                    blocked
            ));
        }

        String recommended = eligible.stream().sorted(byScoreThenTco).map(CandidateInput::supplierId).findFirst().orElse(null);

        List<String> missing = new ArrayList<>();
        for (CandidateInput c : normalized) {
            if (c.missingDataHints() != null) {
                missing.addAll(c.missingDataHints());
            }
            if (c.overallScore() == null) {
                missing.add("Score missing for " + c.supplierId());
            }
            if (isLifecycleBlocked(c.lifecycleStatus())) {
                missing.add("Lifecycle BLOCKED/SUSPENDED for " + c.supplierId());
            }
        }
        ConfidenceLevel confidence = ConfidenceLevel.HIGH;
        if (!missing.isEmpty() || eligible.size() < normalized.size()) {
            confidence = ConfidenceLevel.MEDIUM;
        }
        if (eligible.isEmpty() || missing.size() >= normalized.size()) {
            confidence = ConfidenceLevel.LOW;
        }

        String rationale;
        if (recommended == null) {
            rationale = "No eligible supplier. Mandatory gates and/or lifecycle BLOCKED/SUSPENDED for all candidates. "
                    + "Human review required; system will not approve.";
        } else {
            rationale = "Draft recommendation: supplier " + recommended
                    + " ranks first among eligible candidates by score then TCO. "
                    + "BLOCKED/SUSPENDED suppliers are excluded from eligibility. "
                    + "This is a decision-support draft only — final approval is human-controlled.";
        }

        return new RankingOutput(recommended, confidence, rationale,
                missing.isEmpty() ? null : String.join("; ", missing.stream().distinct().toList()),
                ranked);
    }

    private CandidateInput applyLifecycleHardStop(CandidateInput c) {
        if (!isLifecycleBlocked(c.lifecycleStatus())) {
            return c;
        }
        List<String> gates = new ArrayList<>();
        if (c.failedGates() != null) {
            gates.addAll(c.failedGates());
        }
        if (!gates.contains("LIFECYCLE_STATUS")) {
            gates.add("LIFECYCLE_STATUS");
        }
        return new CandidateInput(
                c.supplierId(),
                false,
                c.overallScore(),
                c.tcoTotal(),
                c.tcoDataComplete(),
                gates,
                c.missingDataHints(),
                c.lifecycleStatus()
        );
    }

    static boolean isLifecycleBlocked(String lifecycleStatus) {
        if (lifecycleStatus == null || lifecycleStatus.isBlank()) {
            return false;
        }
        String s = lifecycleStatus.trim().toUpperCase();
        return "BLOCKED".equals(s) || "SUSPENDED".equals(s);
    }

    private static String buildStrengths(CandidateInput c) {
        List<String> s = new ArrayList<>();
        if (c.eligible()) s.add("Eligible");
        if (c.overallScore() != null) s.add("Score " + c.overallScore());
        if (c.tcoDataComplete() && c.tcoTotal() != null) s.add("TCO complete " + c.tcoTotal());
        return String.join("; ", s);
    }

    private static String buildWeaknesses(CandidateInput c) {
        List<String> w = new ArrayList<>();
        if (!c.eligible()) {
            w.add("Ineligible");
            if (c.failedGates() != null && !c.failedGates().isEmpty()) {
                w.add("Failed gates: " + String.join(",", c.failedGates()));
            }
        }
        if (isLifecycleBlocked(c.lifecycleStatus())) {
            w.add("Lifecycle " + c.lifecycleStatus());
        }
        if (!c.tcoDataComplete()) w.add("TCO incomplete");
        if (c.overallScore() == null) w.add("No scorecard score");
        return w.isEmpty() ? null : String.join("; ", w);
    }
}
