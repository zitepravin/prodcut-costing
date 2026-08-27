package com.automotive.costing.vendorselection.engine;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds a side-by-side comparison matrix. Does not rank for approval —
 * surfaces trade-offs (score vs TCO vs eligibility vs evidence gaps).
 * BLOCKED / SUSPENDED lifecycle forces ineligible (V60) — aligned with ranking/eligibility.
 */
@Component
public class SupplierComparisonEngine {

    public record SupplierFacts(
            String supplierId,
            String supplierName,
            boolean eligible,
            List<String> failedGates,
            BigDecimal overallScore,
            BigDecimal tcoTotal,
            boolean tcoDataComplete,
            List<String> tcoMissingComponents,
            BigDecimal latestPpm,
            BigDecimal latestOtdPercent,
            Integer warrantyMonths,
            int activeCapabilityCount,
            List<String> dataGaps,
            String lifecycleStatus
    ) {}

    public record ComparisonRow(
            String dimension,
            String nature,
            List<String> values
    ) {}

    public record ComparisonOutput(
            List<String> supplierIds,
            List<ComparisonRow> rows,
            String tradeOffSummary,
            List<String> lifecycleBlockedSupplierIds
    ) {}

    public ComparisonOutput compare(List<SupplierFacts> facts) {
        if (facts == null || facts.isEmpty()) {
            return new ComparisonOutput(List.of(), List.of(),
                    "No suppliers to compare. This comparison does not approve or award business.",
                    List.of());
        }

        List<SupplierFacts> normalized = facts.stream().map(this::applyLifecycleHardStop).toList();

        List<String> ids = normalized.stream().map(SupplierFacts::supplierId).toList();
        List<String> blockedIds = normalized.stream()
                .filter(f -> isLifecycleBlocked(f.lifecycleStatus()))
                .map(SupplierFacts::supplierId)
                .toList();

        List<ComparisonRow> rows = new ArrayList<>();

        rows.add(row("SUPPLIER_NAME", "FACT", normalized.stream()
                .map(f -> f.supplierName() == null ? f.supplierId() : f.supplierName()).toList()));
        rows.add(row("LIFECYCLE_STATUS", "FACT", normalized.stream()
                .map(f -> f.lifecycleStatus() == null ? "DATA_NOT_AVAILABLE" : f.lifecycleStatus()).toList()));
        rows.add(row("ELIGIBLE", "CALCULATED", normalized.stream()
                .map(f -> Boolean.toString(f.eligible())).toList()));
        rows.add(row("FAILED_GATES", "CALCULATED", normalized.stream()
                .map(f -> f.failedGates() == null || f.failedGates().isEmpty()
                        ? "none" : String.join(",", f.failedGates())).toList()));
        rows.add(row("OVERALL_SCORE", "CALCULATED", normalized.stream()
                .map(f -> f.overallScore() == null ? "DATA_NOT_AVAILABLE" : f.overallScore().toPlainString()).toList()));
        rows.add(row("TCO_TOTAL", "CALCULATED", normalized.stream()
                .map(f -> f.tcoTotal() == null ? "DATA_NOT_AVAILABLE" : f.tcoTotal().toPlainString()).toList()));
        rows.add(row("TCO_COMPLETE", "FACT", normalized.stream()
                .map(f -> Boolean.toString(f.tcoDataComplete())).toList()));
        rows.add(row("TCO_MISSING", "MISSING_DATA", normalized.stream()
                .map(f -> f.tcoMissingComponents() == null || f.tcoMissingComponents().isEmpty()
                        ? "none" : String.join(",", f.tcoMissingComponents())).toList()));
        rows.add(row("LATEST_PPM", "FACT", normalized.stream()
                .map(f -> f.latestPpm() == null ? "DATA_NOT_AVAILABLE" : f.latestPpm().toPlainString()).toList()));
        rows.add(row("LATEST_OTD_PERCENT", "FACT", normalized.stream()
                .map(f -> f.latestOtdPercent() == null ? "DATA_NOT_AVAILABLE" : f.latestOtdPercent().toPlainString()).toList()));
        rows.add(row("WARRANTY_MONTHS", "FACT", normalized.stream()
                .map(f -> f.warrantyMonths() == null ? "DATA_NOT_AVAILABLE" : f.warrantyMonths().toString()).toList()));
        rows.add(row("ACTIVE_CAPABILITIES", "FACT", normalized.stream()
                .map(f -> Integer.toString(f.activeCapabilityCount())).toList()));
        rows.add(row("DATA_GAPS", "MISSING_DATA", normalized.stream()
                .map(f -> f.dataGaps() == null || f.dataGaps().isEmpty()
                        ? "none" : String.join("; ", f.dataGaps())).toList()));

        String summary = buildTradeOffSummary(normalized, blockedIds);
        return new ComparisonOutput(ids, rows, summary, blockedIds);
    }

    private SupplierFacts applyLifecycleHardStop(SupplierFacts f) {
        if (!isLifecycleBlocked(f.lifecycleStatus())) {
            return f;
        }
        List<String> gates = new ArrayList<>();
        if (f.failedGates() != null) {
            gates.addAll(f.failedGates());
        }
        if (!gates.contains("LIFECYCLE_STATUS")) {
            gates.add("LIFECYCLE_STATUS");
        }
        List<String> gaps = new ArrayList<>();
        if (f.dataGaps() != null) {
            gaps.addAll(f.dataGaps());
        }
        gaps.add("Lifecycle " + f.lifecycleStatus());
        return new SupplierFacts(
                f.supplierId(),
                f.supplierName(),
                false,
                gates,
                f.overallScore(),
                f.tcoTotal(),
                f.tcoDataComplete(),
                f.tcoMissingComponents(),
                f.latestPpm(),
                f.latestOtdPercent(),
                f.warrantyMonths(),
                f.activeCapabilityCount(),
                gaps,
                f.lifecycleStatus()
        );
    }

    static boolean isLifecycleBlocked(String lifecycleStatus) {
        if (lifecycleStatus == null || lifecycleStatus.isBlank()) {
            return false;
        }
        String s = lifecycleStatus.trim().toUpperCase();
        return "BLOCKED".equals(s) || "SUSPENDED".equals(s);
    }

    private static ComparisonRow row(String dim, String nature, List<String> values) {
        return new ComparisonRow(dim, nature, values);
    }

    private String buildTradeOffSummary(List<SupplierFacts> facts, List<String> blockedIds) {
        StringBuilder sb = new StringBuilder();
        if (!blockedIds.isEmpty()) {
            sb.append("Lifecycle BLOCKED/SUSPENDED suppliers are ineligible for preference: ")
                    .append(String.join(",", blockedIds)).append(". ");
        }
        List<SupplierFacts> eligible = facts.stream().filter(SupplierFacts::eligible).toList();
        SupplierFacts bestScore = eligible.stream()
                .filter(f -> f.overallScore() != null)
                .max(Comparator.comparing(SupplierFacts::overallScore))
                .orElse(null);
        SupplierFacts bestTco = eligible.stream()
                .filter(f -> f.tcoTotal() != null && f.tcoDataComplete())
                .min(Comparator.comparing(SupplierFacts::tcoTotal))
                .orElse(null);
        if (bestScore != null) {
            sb.append("Among eligible, highest score is ").append(bestScore.supplierId())
                    .append(" (").append(bestScore.overallScore()).append("). ");
        }
        if (bestTco != null) {
            sb.append("Lowest complete TCO among eligible is ").append(bestTco.supplierId())
                    .append(" (").append(bestTco.tcoTotal()).append("). ");
        }
        if (bestScore != null && bestTco != null
                && !bestScore.supplierId().equals(bestTco.supplierId())) {
            sb.append("Score leader and TCO leader differ — human trade-off required. ");
        }
        if (eligible.isEmpty()) {
            sb.append("No eligible suppliers in this set. ");
        }
        sb.append("This comparison does not approve or award business.");
        return sb.toString();
    }
}
