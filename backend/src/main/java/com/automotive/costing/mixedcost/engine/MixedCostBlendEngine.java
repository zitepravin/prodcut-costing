package com.automotive.costing.mixedcost.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure engine for P2-03 mixed costing.
 * <ul>
 *   <li>Ratios must sum to 1.0000 (tolerance 0.00005, scale 4 HALF_UP)</li>
 *   <li>Blended unit = Σ (snapshot × ratio) only over ELIGIBLE + AVAILABLE legs</li>
 *   <li>Any leg NOT_AVAILABLE or NOT_ELIGIBLE → blended_status NOT_AVAILABLE (v1 strict)</li>
 *   <li>Never invents prices; never treats missing as zero while claiming AVAILABLE</li>
 * </ul>
 */
public final class MixedCostBlendEngine {

    public static final BigDecimal ONE = BigDecimal.ONE;
    public static final BigDecimal RATIO_TOLERANCE = new BigDecimal("0.00005");
    public static final int RATIO_SCALE = 4;
    public static final int MONEY_SCALE = 6;
    public static final RoundingMode RM = RoundingMode.HALF_UP;

    public static final String TYPE_INTERNAL = "INTERNAL_SHOULD_COST";
    public static final String TYPE_QUOTE = "SUPPLIER_QUOTE";

    public static final String ELIGIBLE = "ELIGIBLE";
    public static final String NOT_ELIGIBLE = "NOT_ELIGIBLE";
    public static final String UNKNOWN = "UNKNOWN";

    public static final String AVAILABLE = "AVAILABLE";
    public static final String NOT_AVAILABLE = "NOT_AVAILABLE";

    private MixedCostBlendEngine() {
    }

    public record LegInput(
            int legSeq,
            String alternativeType,
            String sourceRefId,
            String supplierId,
            BigDecimal mixRatio,
            BigDecimal unitCost,
            String currency,
            String eligibilityStatus,
            String eligibilityNotes,
            String dataStatus
    ) {
    }

    public record BlendResult(
            boolean ratioValid,
            BigDecimal ratioSum,
            String blendedStatus,
            BigDecimal blendedUnitTotal,
            List<String> dataQualityNotes,
            List<LegSnapshot> legSnapshots,
            boolean calculateBlocked
    ) {
    }

    public record LegSnapshot(
            int legSeq,
            String alternativeType,
            String sourceRefId,
            String supplierId,
            BigDecimal mixRatio,
            BigDecimal unitCostSnapshot,
            String currencySnapshot,
            String eligibilityStatus,
            String eligibilityNotes,
            String dataStatus
    ) {
    }

    /**
     * Validate ratios and compute blend under v1 strict policy.
     */
    public static BlendResult calculate(List<LegInput> legs) {
        List<String> notes = new ArrayList<>();
        List<LegSnapshot> snapshots = new ArrayList<>();

        if (legs == null || legs.size() < 2) {
            notes.add("At least two legs are required for mixed costing.");
            return new BlendResult(false, null, NOT_AVAILABLE, null, notes, snapshots, true);
        }

        BigDecimal ratioSum = BigDecimal.ZERO;
        for (LegInput leg : legs) {
            if (leg.mixRatio() == null || leg.mixRatio().compareTo(BigDecimal.ZERO) <= 0) {
                notes.add("Leg " + leg.legSeq() + ": mix_ratio must be > 0.");
                return new BlendResult(false, null, NOT_AVAILABLE, null, notes, snapshots, true);
            }
            ratioSum = ratioSum.add(leg.mixRatio());
        }
        ratioSum = ratioSum.setScale(RATIO_SCALE, RM);
        boolean ratioValid = ratioSum.subtract(ONE).abs().compareTo(RATIO_TOLERANCE) <= 0;
        if (!ratioValid) {
            notes.add("Mix ratios must sum to 100% (1.0000). Current sum=" + ratioSum.toPlainString());
            return new BlendResult(false, ratioSum, NOT_AVAILABLE, null, notes, snapshots, true);
        }

        boolean anyBlocked = false;
        boolean anyMissing = false;
        BigDecimal blended = BigDecimal.ZERO;

        for (LegInput leg : legs) {
            String elig = leg.eligibilityStatus() != null ? leg.eligibilityStatus() : UNKNOWN;
            String data = leg.dataStatus() != null ? leg.dataStatus() : NOT_AVAILABLE;
            BigDecimal unit = leg.unitCost();

            if (NOT_ELIGIBLE.equals(elig)) {
                anyBlocked = true;
                notes.add("Leg " + leg.legSeq() + ": supplier not eligible"
                        + (leg.eligibilityNotes() != null ? " (" + leg.eligibilityNotes() + ")" : "")
                        + " — calculate blocked.");
            }
            if (!AVAILABLE.equals(data) || unit == null) {
                anyMissing = true;
                notes.add("Leg " + leg.legSeq() + ": unit cost DATA NOT AVAILABLE.");
                data = NOT_AVAILABLE;
                unit = null;
            }

            snapshots.add(new LegSnapshot(
                    leg.legSeq(),
                    leg.alternativeType(),
                    leg.sourceRefId(),
                    leg.supplierId(),
                    leg.mixRatio().setScale(RATIO_SCALE, RM),
                    unit != null ? unit.setScale(MONEY_SCALE, RM) : null,
                    leg.currency(),
                    elig,
                    leg.eligibilityNotes(),
                    data
            ));

            if (AVAILABLE.equals(data) && ELIGIBLE.equals(elig) && unit != null) {
                blended = blended.add(unit.multiply(leg.mixRatio()));
            }
        }

        if (anyBlocked || anyMissing) {
            return new BlendResult(true, ratioSum, NOT_AVAILABLE, null, notes, snapshots, anyBlocked || anyMissing);
        }

        blended = blended.setScale(MONEY_SCALE, RM);
        notes.add("Blended unit cost computed from " + legs.size() + " eligible legs.");
        return new BlendResult(true, ratioSum, AVAILABLE, blended, notes, snapshots, false);
    }
}
