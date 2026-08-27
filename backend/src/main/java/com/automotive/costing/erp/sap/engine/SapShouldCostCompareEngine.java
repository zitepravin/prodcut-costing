package com.automotive.costing.erp.sap.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure comparison of SAP cost estimate vs should-cost.
 * Never invents missing amounts as zero while claiming AVAILABLE.
 */
public final class SapShouldCostCompareEngine {

    public static final int MONEY_SCALE = 6;
    public static final RoundingMode RM = RoundingMode.HALF_UP;

    public static final String AVAILABLE = "AVAILABLE";
    public static final String NOT_AVAILABLE = "NOT_AVAILABLE";
    public static final String PARTIAL = "PARTIAL";

    private SapShouldCostCompareEngine() {
    }

    public record ComponentLine(
            String code,
            BigDecimal sapAmount,
            BigDecimal shouldAmount,
            BigDecimal variance,
            String status
    ) {
    }

    public record CompareResult(
            String totalStatus,
            BigDecimal sapTotal,
            BigDecimal shouldTotal,
            BigDecimal absoluteVariance,
            BigDecimal percentageVariance,
            List<ComponentLine> components,
            List<String> dataQualityNotes
    ) {
    }

    /**
     * @param sapTotal           may be null
     * @param shouldTotal        may be null
     * @param sapByOurCode       amounts keyed by OUR component codes (after mapping)
     * @param shouldByOurCode    amounts keyed by OUR component codes
     */
    public static CompareResult compare(
            BigDecimal sapTotal,
            BigDecimal shouldTotal,
            Map<String, BigDecimal> sapByOurCode,
            Map<String, BigDecimal> shouldByOurCode
    ) {
        List<String> notes = new ArrayList<>();
        List<ComponentLine> lines = new ArrayList<>();

        if (sapTotal == null) {
            notes.add("SAP total cost DATA NOT AVAILABLE.");
        }
        if (shouldTotal == null) {
            notes.add("Should-cost total DATA NOT AVAILABLE.");
        }

        Map<String, Boolean> codes = new LinkedHashMap<>();
        if (sapByOurCode != null) {
            sapByOurCode.keySet().forEach(c -> codes.put(c, Boolean.TRUE));
        }
        if (shouldByOurCode != null) {
            shouldByOurCode.keySet().forEach(c -> codes.put(c, Boolean.TRUE));
        }

        for (String code : codes.keySet()) {
            BigDecimal sap = sapByOurCode != null ? sapByOurCode.get(code) : null;
            BigDecimal sc = shouldByOurCode != null ? shouldByOurCode.get(code) : null;
            if (sap == null && sc == null) {
                lines.add(new ComponentLine(code, null, null, null, NOT_AVAILABLE));
                notes.add("Component " + code + ": both sides DATA NOT AVAILABLE.");
            } else if (sap == null) {
                lines.add(new ComponentLine(code, null, scale(sc), null, NOT_AVAILABLE));
                notes.add("Component " + code + ": SAP amount DATA NOT AVAILABLE.");
            } else if (sc == null) {
                lines.add(new ComponentLine(code, scale(sap), null, null, NOT_AVAILABLE));
                notes.add("Component " + code + ": should-cost amount DATA NOT AVAILABLE.");
            } else {
                BigDecimal var = scale(sap.subtract(sc));
                lines.add(new ComponentLine(code, scale(sap), scale(sc), var, AVAILABLE));
            }
        }

        String totalStatus;
        BigDecimal absVar = null;
        BigDecimal pctVar = null;
        if (sapTotal == null || shouldTotal == null) {
            totalStatus = NOT_AVAILABLE;
        } else {
            absVar = scale(sapTotal.subtract(shouldTotal));
            if (shouldTotal.compareTo(BigDecimal.ZERO) != 0) {
                pctVar = absVar.multiply(new BigDecimal("100"))
                        .divide(shouldTotal, 4, RM);
            } else {
                notes.add("Should-cost total is zero; percentage variance NOT AVAILABLE.");
            }
            totalStatus = codes.isEmpty() ? AVAILABLE : (
                    lines.stream().allMatch(l -> AVAILABLE.equals(l.status())) ? AVAILABLE : PARTIAL
            );
        }

        return new CompareResult(
                totalStatus,
                scale(sapTotal),
                scale(shouldTotal),
                absVar,
                pctVar,
                lines,
                notes
        );
    }

    private static BigDecimal scale(BigDecimal v) {
        return v == null ? null : v.setScale(MONEY_SCALE, RM);
    }
}
