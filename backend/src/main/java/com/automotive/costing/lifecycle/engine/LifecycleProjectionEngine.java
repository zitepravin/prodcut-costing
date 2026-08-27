package com.automotive.costing.lifecycle.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure lifecycle projection. v1: volume-insensitive unit cost held constant;
 * tooling re-amortized by curve lifetime volume. Missing inputs stay null (NOT AVAILABLE).
 */
public final class LifecycleProjectionEngine {

    public static final int MONEY_SCALE = 6;
    public static final int VOLUME_SCALE = 2;

    private LifecycleProjectionEngine() {
    }

    public record YearVolume(int yearIndex, Integer calendarYear, BigDecimal plannedVolume) {
    }

    public record YearProjection(
            int yearIndex,
            Integer calendarYear,
            BigDecimal plannedVolume,
            BigDecimal unitCost,
            BigDecimal yearTotalCost,
            BigDecimal cumulativeVolume,
            BigDecimal cumulativeCost
    ) {
    }

    public record ProjectionResult(
            BigDecimal volumeInsensitiveUnit,
            BigDecimal toolingPerUnit,
            String toolingStatus,
            BigDecimal unitCostBaseline,
            String unitCostStatus,
            BigDecimal lifetimeVolumeForCurve,
            List<YearProjection> years,
            List<String> dataQualityNotes
    ) {
    }

    /**
     * @param volumeInsensitiveUnit fixed per-unit (material+process+… excluding tooling)
     * @param toolingCash           total tooling cash to amortize over curve lifetime (nullable)
     * @param years                 planned volumes
     */
    public static ProjectionResult project(
            BigDecimal volumeInsensitiveUnit,
            BigDecimal toolingCash,
            List<YearVolume> years
    ) {
        List<String> notes = new ArrayList<>();
        if (years == null || years.isEmpty()) {
            notes.add("No volume curve years defined.");
            return new ProjectionResult(null, null, "NOT_AVAILABLE", null, "NOT_AVAILABLE",
                    null, List.of(), notes);
        }
        if (volumeInsensitiveUnit == null && toolingCash == null) {
            notes.add("No unit cost baseline available.");
            return new ProjectionResult(null, null, "NOT_AVAILABLE", null, "NOT_AVAILABLE",
                    null, List.of(), notes);
        }

        BigDecimal lifetime = BigDecimal.ZERO;
        for (YearVolume y : years) {
            if (y.plannedVolume() != null) {
                lifetime = lifetime.add(y.plannedVolume());
            }
        }
        lifetime = lifetime.setScale(VOLUME_SCALE, RoundingMode.HALF_UP);

        BigDecimal toolingPerUnit = null;
        String toolingStatus;
        if (toolingCash == null) {
            toolingStatus = "NOT_AVAILABLE";
            notes.add("Tooling cash unknown; tooling not re-amortized.");
        } else if (lifetime.compareTo(BigDecimal.ZERO) <= 0) {
            toolingStatus = "NOT_AVAILABLE";
            notes.add("Curve lifetime volume is zero; cannot amortize tooling.");
        } else {
            toolingPerUnit = toolingCash.divide(lifetime, MONEY_SCALE, RoundingMode.HALF_UP);
            toolingStatus = "AVAILABLE";
        }

        BigDecimal ins = volumeInsensitiveUnit != null
                ? volumeInsensitiveUnit.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (volumeInsensitiveUnit == null) {
            notes.add("Volume-insensitive unit cost missing; treated as 0 for sum only when tooling exists.");
        }

        BigDecimal unitBaseline = ins;
        if (toolingPerUnit != null) {
            unitBaseline = ins.add(toolingPerUnit).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        String unitStatus = (volumeInsensitiveUnit == null && toolingPerUnit == null)
                ? "NOT_AVAILABLE"
                : (toolingStatus.equals("NOT_AVAILABLE") && volumeInsensitiveUnit != null ? "PARTIAL" : "AVAILABLE");
        if (volumeInsensitiveUnit == null && toolingPerUnit != null) {
            unitStatus = "PARTIAL";
        }

        List<YearVolume> ordered = years.stream()
                .sorted(Comparator.comparingInt(YearVolume::yearIndex))
                .toList();

        List<YearProjection> out = new ArrayList<>();
        BigDecimal cumVol = BigDecimal.ZERO;
        BigDecimal cumCost = BigDecimal.ZERO;
        for (YearVolume y : ordered) {
            BigDecimal vol = y.plannedVolume() == null
                    ? BigDecimal.ZERO
                    : y.plannedVolume().setScale(VOLUME_SCALE, RoundingMode.HALF_UP);
            BigDecimal unit = unitBaseline;
            BigDecimal yearTotal = unit.multiply(vol).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            cumVol = cumVol.add(vol).setScale(VOLUME_SCALE, RoundingMode.HALF_UP);
            cumCost = cumCost.add(yearTotal).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            out.add(new YearProjection(
                    y.yearIndex(),
                    y.calendarYear(),
                    vol,
                    unit,
                    yearTotal,
                    cumVol,
                    cumCost
            ));
        }

        return new ProjectionResult(
                volumeInsensitiveUnit != null ? ins : null,
                toolingPerUnit,
                toolingStatus,
                "NOT_AVAILABLE".equals(unitStatus) ? null : unitBaseline,
                unitStatus,
                lifetime,
                out,
                notes
        );
    }

    /** Reconstruct tooling cash from baseline per-unit tooling × baseline lifetime volume. */
    public static BigDecimal toolingCash(BigDecimal toolingPerUnitBaseline, BigDecimal baselineLifetimeVolume) {
        if (toolingPerUnitBaseline == null || baselineLifetimeVolume == null) {
            return null;
        }
        if (baselineLifetimeVolume.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return toolingPerUnitBaseline.multiply(baselineLifetimeVolume).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
