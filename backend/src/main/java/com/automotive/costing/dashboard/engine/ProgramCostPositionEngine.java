package com.automotive.costing.dashboard.engine;

import com.automotive.costing.shouldcost.entity.ApprovalStatus;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pure program cost-position helpers. One selected should-cost row per BOM node —
 * never sum all scenarios (SC-06). Missing sides stay null (NOT AVAILABLE), never zeroed.
 */
public final class ProgramCostPositionEngine {

    public static final int SCALE = 6;
    public static final String POLICY_DEFAULT = "APPROVED_THEN_CURRENT_THEN_LATEST";
    public static final String POLICY_APPROVED_ONLY = "APPROVED_ONLY";
    public static final String DEFAULT_SCENARIO = "Current Design";

    private ProgramCostPositionEngine() {
    }

    public record SelectedBuildUp(ShouldCostBuildUp buildUp) {
    }

    public record DriverSum(
            BigDecimal material,
            BigDecimal process,
            BigDecimal tooling,
            BigDecimal packagingFreight,
            BigDecimal overhead,
            BigDecimal sga,
            BigDecimal profitMargin,
            BigDecimal total
    ) {
    }

    public record VarianceResult(
            BigDecimal absoluteVariance,
            BigDecimal percentageVariance
    ) {
    }

    /**
     * Select at most one build-up per bomNodeId according to policy.
     */
    public static List<ShouldCostBuildUp> selectOnePerBomNode(
            List<ShouldCostBuildUp> candidates,
            String policy
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        String effectivePolicy = policy == null || policy.isBlank() ? POLICY_DEFAULT : policy;
        Map<String, List<ShouldCostBuildUp>> byNode = new HashMap<>();
        for (ShouldCostBuildUp b : candidates) {
            if (b == null || b.getBomNodeId() == null) {
                continue;
            }
            byNode.computeIfAbsent(b.getBomNodeId(), k -> new ArrayList<>()).add(b);
        }
        List<ShouldCostBuildUp> selected = new ArrayList<>();
        for (List<ShouldCostBuildUp> group : byNode.values()) {
            ShouldCostBuildUp pick = pickOne(group, effectivePolicy);
            if (pick != null) {
                selected.add(pick);
            }
        }
        return selected;
    }

    private static ShouldCostBuildUp pickOne(List<ShouldCostBuildUp> group, String policy) {
        if (group.isEmpty()) {
            return null;
        }
        Comparator<ShouldCostBuildUp> byCalcDesc = Comparator
                .comparing(ShouldCostBuildUp::getCalculatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed();

        if (POLICY_APPROVED_ONLY.equals(policy)) {
            return group.stream()
                    .filter(b -> b.getApprovalStatus() == ApprovalStatus.APPROVED)
                    .sorted(byCalcDesc)
                    .findFirst()
                    .orElse(null);
        }

        // APPROVED_THEN_CURRENT_THEN_LATEST
        var approved = group.stream()
                .filter(b -> b.getApprovalStatus() == ApprovalStatus.APPROVED)
                .sorted(byCalcDesc)
                .findFirst();
        if (approved.isPresent()) {
            return approved.get();
        }
        var current = group.stream()
                .filter(b -> DEFAULT_SCENARIO.equalsIgnoreCase(Objects.toString(b.getScenarioLabel(), "")))
                .sorted(byCalcDesc)
                .findFirst();
        if (current.isPresent()) {
            return current.get();
        }
        return group.stream().sorted(byCalcDesc).findFirst().orElse(null);
    }

    public static DriverSum sumDrivers(List<ShouldCostBuildUp> selected) {
        BigDecimal material = BigDecimal.ZERO;
        BigDecimal process = BigDecimal.ZERO;
        BigDecimal tooling = BigDecimal.ZERO;
        BigDecimal packaging = BigDecimal.ZERO;
        BigDecimal overhead = BigDecimal.ZERO;
        BigDecimal sga = BigDecimal.ZERO;
        BigDecimal margin = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (ShouldCostBuildUp b : selected) {
            material = material.add(nz(b.getMaterialCost()));
            process = process.add(nz(b.getProcessCost()));
            tooling = tooling.add(nz(b.getToolingCost()));
            packaging = packaging.add(nz(b.getPackagingFreightCost()));
            overhead = overhead.add(nz(b.getOverheadCost()));
            sga = sga.add(nz(b.getSgaCost()));
            margin = margin.add(nz(b.getProfitMargin()));
            total = total.add(nz(b.getTotalCost()));
        }
        return new DriverSum(
                material.setScale(SCALE, RoundingMode.HALF_UP),
                process.setScale(SCALE, RoundingMode.HALF_UP),
                tooling.setScale(SCALE, RoundingMode.HALF_UP),
                packaging.setScale(SCALE, RoundingMode.HALF_UP),
                overhead.setScale(SCALE, RoundingMode.HALF_UP),
                sga.setScale(SCALE, RoundingMode.HALF_UP),
                margin.setScale(SCALE, RoundingMode.HALF_UP),
                total.setScale(SCALE, RoundingMode.HALF_UP)
        );
    }

    /**
     * absolute = should − target; percentage = absolute / target (null if target is null or zero).
     */
    public static VarianceResult variance(BigDecimal shouldCostTotal, BigDecimal targetTotal) {
        if (shouldCostTotal == null || targetTotal == null) {
            return null;
        }
        BigDecimal absolute = shouldCostTotal.subtract(targetTotal).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal percentage = null;
        if (targetTotal.compareTo(BigDecimal.ZERO) != 0) {
            percentage = absolute.divide(targetTotal, SCALE, RoundingMode.HALF_UP);
        }
        return new VarianceResult(absolute, percentage);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
