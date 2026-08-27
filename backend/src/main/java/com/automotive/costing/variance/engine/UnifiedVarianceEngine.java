package com.automotive.costing.variance.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * B3 pure variance arithmetic — explains cost differences by driver where data exists.
 */
public final class UnifiedVarianceEngine {

    private static final int SCALE = 6;

    private UnifiedVarianceEngine() {}

    public record CostSnapshot(
            BigDecimal material,
            BigDecimal process,
            BigDecimal tooling,
            BigDecimal packaging,
            BigDecimal overhead,
            BigDecimal sga,
            BigDecimal margin,
            BigDecimal total
    ) {
        public static CostSnapshot ofTotal(BigDecimal total) {
            return new CostSnapshot(null, null, null, null, null, null, null, total);
        }
    }

    public record DriverLine(
            String driverCode,
            String driverName,
            BigDecimal baselineAmount,
            BigDecimal comparisonAmount,
            BigDecimal absoluteVariance,
            BigDecimal percentVariance,
            String explanation
    ) {}

    public record VarianceResult(
            BigDecimal baselineTotal,
            BigDecimal comparisonTotal,
            BigDecimal absoluteVariance,
            BigDecimal percentVariance,
            List<DriverLine> drivers,
            String summary
    ) {}

    public static VarianceResult compare(CostSnapshot baseline, CostSnapshot comparison, String comparisonLabel) {
        BigDecimal bTotal = totalOf(baseline);
        BigDecimal cTotal = totalOf(comparison);
        BigDecimal abs = cTotal.subtract(bTotal).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal pct = percent(abs, bTotal);

        List<DriverLine> drivers = new ArrayList<>();
        addDriver(drivers, "MATERIAL", "Material", baseline.material(), comparison.material());
        addDriver(drivers, "PROCESS", "Process / labor+machine", baseline.process(), comparison.process());
        addDriver(drivers, "TOOLING", "Tooling", baseline.tooling(), comparison.tooling());
        addDriver(drivers, "PACKAGING", "Packaging & freight", baseline.packaging(), comparison.packaging());
        addDriver(drivers, "OVERHEAD", "Overhead", baseline.overhead(), comparison.overhead());
        addDriver(drivers, "SGA", "SG&A", baseline.sga(), comparison.sga());
        addDriver(drivers, "MARGIN", "Profit margin", baseline.margin(), comparison.margin());

        // If only totals available, single TOTAL driver
        if (drivers.isEmpty()) {
            drivers.add(new DriverLine(
                    "TOTAL", "Total cost", bTotal, cTotal, abs, pct,
                    "Component breakdown not available — total variance only"
            ));
        }

        String summary = "Comparison vs " + comparisonLabel + ": absolute=" + abs.toPlainString()
                + ", percent=" + pct.toPlainString()
                + (abs.compareTo(BigDecimal.ZERO) > 0 ? " (comparison higher)" :
                   abs.compareTo(BigDecimal.ZERO) < 0 ? " (comparison lower)" : " (equal)");

        return new VarianceResult(bTotal, cTotal, abs, pct, drivers, summary);
    }

    private static void addDriver(List<DriverLine> drivers, String code, String name, BigDecimal b, BigDecimal c) {
        if (b == null && c == null) {
            return;
        }
        BigDecimal bb = nz(b);
        BigDecimal cc = nz(c);
        BigDecimal abs = cc.subtract(bb).setScale(SCALE, RoundingMode.HALF_UP);
        if (abs.compareTo(BigDecimal.ZERO) == 0 && bb.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        drivers.add(new DriverLine(
                code, name, bb.setScale(SCALE, RoundingMode.HALF_UP), cc.setScale(SCALE, RoundingMode.HALF_UP),
                abs, percent(abs, bb),
                code + " variance " + abs.toPlainString()
        ));
    }

    private static BigDecimal totalOf(CostSnapshot s) {
        if (s.total() != null) {
            return s.total().setScale(SCALE, RoundingMode.HALF_UP);
        }
        return nz(s.material()).add(nz(s.process())).add(nz(s.tooling()))
                .add(nz(s.packaging())).add(nz(s.overhead())).add(nz(s.sga())).add(nz(s.margin()))
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal percent(BigDecimal abs, BigDecimal base) {
        if (base == null || base.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return abs.multiply(new BigDecimal("100")).divide(base, SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
