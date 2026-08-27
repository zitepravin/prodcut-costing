package com.automotive.costing.makevsbuy.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Market-aligned Make-vs-Buy draft recommendation engine.
 * <ul>
 *   <li>NOT_ELIGIBLE legs never win recommendation (still visible)</li>
 *   <li>Prefer TCO ranking when all preferred BUY legs have TCO AVAILABLE; else unit cost + warning</li>
 *   <li>Missing data never treated as zero</li>
 *   <li>Output is draft only — human must decide</li>
 * </ul>
 */
public final class MakeVsBuyComparisonEngine {

    public static final String MAKE = "MAKE";
    public static final String BUY = "BUY";
    public static final String MIXED = "MIXED";
    public static final String INCONCLUSIVE = "INCONCLUSIVE";

    public static final String ELIGIBLE = "ELIGIBLE";
    public static final String NOT_ELIGIBLE = "NOT_ELIGIBLE";
    /** Eligibility not yet determined (e.g. supplier master missing). */
    public static final String UNKNOWN = "UNKNOWN";
    public static final String AVAILABLE = "AVAILABLE";
    public static final String NOT_AVAILABLE = "NOT_AVAILABLE";

    public static final int MONEY_SCALE = 6;
    public static final RoundingMode RM = RoundingMode.HALF_UP;

    private MakeVsBuyComparisonEngine() {
    }

    public record LegView(
            int legSeq,
            String strategyType,
            String sourceRefId,
            String supplierId,
            BigDecimal unitCost,
            String currency,
            BigDecimal tcoUnit,
            String tcoStatus,
            String eligibilityStatus,
            String eligibilityNotes,
            String dataStatus,
            String riskSummary
    ) {
    }

    public record Recommendation(
            String recommendationCode,
            String summary,
            List<String> notes,
            String preferredMetric
    ) {
    }

    public static Recommendation recommend(List<LegView> legs) {
        List<String> notes = new ArrayList<>();
        if (legs == null || legs.isEmpty()) {
            notes.add("No strategy legs provided.");
            return new Recommendation(INCONCLUSIVE, "No data to compare.", notes, null);
        }

        LegView make = legs.stream().filter(l -> MAKE.equals(l.strategyType())).findFirst().orElse(null);
        LegView mixed = legs.stream().filter(l -> MIXED.equals(l.strategyType())).findFirst().orElse(null);
        List<LegView> buys = legs.stream().filter(l -> BUY.equals(l.strategyType())).toList();

        List<LegView> preferredBuys = buys.stream()
                .filter(l -> ELIGIBLE.equals(l.eligibilityStatus()) && AVAILABLE.equals(l.dataStatus()) && l.unitCost() != null)
                .toList();

        for (LegView b : buys) {
            if (NOT_ELIGIBLE.equals(b.eligibilityStatus())) {
                notes.add("BUY leg " + b.legSeq() + " is NOT_ELIGIBLE"
                        + (b.eligibilityNotes() != null ? " (" + b.eligibilityNotes() + ")" : "")
                        + " — excluded from recommendation.");
            } else if (!AVAILABLE.equals(b.dataStatus()) || b.unitCost() == null) {
                notes.add("BUY leg " + b.legSeq() + ": unit cost DATA NOT AVAILABLE.");
            }
        }

        boolean makeOk = make != null && AVAILABLE.equals(make.dataStatus()) && make.unitCost() != null;
        boolean mixedOk = mixed != null && AVAILABLE.equals(mixed.dataStatus()) && mixed.unitCost() != null;
        if (make != null && !makeOk) {
            notes.add("MAKE: should-cost DATA NOT AVAILABLE.");
        }
        if (mixed != null && !mixedOk) {
            notes.add("MIXED: blended cost DATA NOT AVAILABLE.");
        }

        if (!makeOk && preferredBuys.isEmpty() && !mixedOk) {
            notes.add("No eligible strategy with available unit cost.");
            return new Recommendation(INCONCLUSIVE, "Inconclusive — insufficient eligible cost data.", notes, null);
        }

        boolean allPreferredHaveTco = !preferredBuys.isEmpty()
                && preferredBuys.stream().allMatch(l -> AVAILABLE.equals(l.tcoStatus()) && l.tcoUnit() != null);

        String metric;
        Comparator<LegView> buyOrder;
        if (allPreferredHaveTco) {
            metric = "TCO";
            buyOrder = Comparator.comparing(LegView::tcoUnit);
            notes.add("Ranking preferred BUY legs by TCO (all preferred have TCO AVAILABLE).");
        } else {
            metric = "UNIT_COST";
            buyOrder = Comparator.comparing(LegView::unitCost);
            if (!preferredBuys.isEmpty()) {
                notes.add("TCO incomplete for preferred BUY set — ranking by unit cost with warning.");
            }
        }

        LegView bestBuy = preferredBuys.stream().min(buyOrder).orElse(null);

        // Build candidates: (code, cost for ranking)
        record Cand(String code, BigDecimal cost) {}
        List<Cand> cands = new ArrayList<>();
        if (makeOk) {
            cands.add(new Cand(MAKE, make.unitCost()));
        }
        if (bestBuy != null) {
            BigDecimal buyCost = allPreferredHaveTco ? bestBuy.tcoUnit() : bestBuy.unitCost();
            cands.add(new Cand(BUY, buyCost));
            notes.add("Best preferred BUY leg seq=" + bestBuy.legSeq()
                    + " unit=" + scale(bestBuy.unitCost())
                    + (allPreferredHaveTco ? " tco=" + scale(bestBuy.tcoUnit()) : ""));
        }
        if (mixedOk) {
            cands.add(new Cand(MIXED, mixed.unitCost()));
        }

        Cand winner = cands.stream().filter(c -> c.cost() != null).min(Comparator.comparing(Cand::cost)).orElse(null);
        if (winner == null) {
            return new Recommendation(INCONCLUSIVE, "Inconclusive — no comparable costs.", notes, metric);
        }

        String summary = "Draft recommendation: " + winner.code()
                + " (metric=" + metric + ", value=" + scale(winner.cost()) + ")."
                + " Human decision required — system does not auto-approve.";
        notes.add("FACT: recommendation is draft only.");
        return new Recommendation(winner.code(), summary, notes, metric);
    }

    private static BigDecimal scale(BigDecimal v) {
        return v == null ? null : v.setScale(MONEY_SCALE, RM);
    }
}
