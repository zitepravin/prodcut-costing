package com.automotive.costing.autointel.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** E3/E4 pure cost impact math — never invents baseline values. */
public final class CostImpactEngine {

    private static final int SCALE = 6;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private CostImpactEngine() {}

    public static BigDecimal unitDelta(BigDecimal baseline, BigDecimal proposed) {
        if (baseline == null || proposed == null) return null;
        return proposed.subtract(baseline).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal annualProgramDelta(BigDecimal unitDelta, BigDecimal annualVolume) {
        if (unitDelta == null || annualVolume == null) return null;
        return unitDelta.multiply(annualVolume).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Apply sequential percentage shocks to a cost structure.
     * Material/process/tooling portions are adjusted independently then summed with residual.
     * If component split is unavailable, apply a combined factor on total only.
     */
    public static BigDecimal simulateUnitCost(
            BigDecimal baselineTotal,
            BigDecimal materialCost,
            BigDecimal processCost,
            BigDecimal toolingCost,
            BigDecimal materialPct,
            BigDecimal processPct,
            BigDecimal toolingPct,
            BigDecimal fxPct) {

        if (baselineTotal == null) return null;

        BigDecimal mat = nz(materialCost);
        BigDecimal proc = nz(processCost);
        BigDecimal tool = nz(toolingCost);
        BigDecimal known = mat.add(proc).add(tool);
        BigDecimal residual = baselineTotal.subtract(known);

        if (known.compareTo(BigDecimal.ZERO) == 0) {
            // no component split — apply fx + average of non-null pcts on total
            BigDecimal factor = BigDecimal.ONE;
            factor = factor.multiply(pctFactor(materialPct));
            factor = factor.multiply(pctFactor(processPct));
            factor = factor.multiply(pctFactor(toolingPct));
            factor = factor.multiply(pctFactor(fxPct));
            return baselineTotal.multiply(factor).setScale(SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal newMat = mat.multiply(pctFactor(materialPct)).multiply(pctFactor(fxPct));
        BigDecimal newProc = proc.multiply(pctFactor(processPct)).multiply(pctFactor(fxPct));
        BigDecimal newTool = tool.multiply(pctFactor(toolingPct)).multiply(pctFactor(fxPct));
        BigDecimal newResidual = residual.multiply(pctFactor(fxPct));
        return newMat.add(newProc).add(newTool).add(newResidual).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** Volume shock applies to annual impact only, not unit cost. */
    public static BigDecimal adjustVolume(BigDecimal baselineVolume, BigDecimal volumePct) {
        if (baselineVolume == null) return null;
        return baselineVolume.multiply(pctFactor(volumePct)).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal pctFactor(BigDecimal pctChange) {
        if (pctChange == null) return BigDecimal.ONE;
        return BigDecimal.ONE.add(pctChange.divide(HUNDRED, 10, RoundingMode.HALF_UP));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
