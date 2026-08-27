package com.automotive.costing.costingsheet.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.costingsheet.entity.CostingSheetBaseCode;
import com.automotive.costing.costingsheet.entity.CostingSheetRule;
import com.automotive.costing.costingsheet.entity.CostingSheetRuleType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * A6 pure calculation engine: apply sequential costing-sheet rules to a cost base.
 * Backend is the financial source of truth — no UI-only arithmetic.
 */
public final class CostingSheetEngine {

    private static final int SCALE = 6;

    private CostingSheetEngine() {}

    public record CostBase(
            BigDecimal material,
            BigDecimal process,
            BigDecimal tooling,
            BigDecimal packaging,
            BigDecimal directLabor,
            BigDecimal machine
    ) {
        public CostBase(BigDecimal material, BigDecimal process, BigDecimal tooling, BigDecimal packaging) {
            this(material, process, tooling, packaging, null, null);
        }
        public BigDecimal subtotal() {
            return nz(material).add(nz(process)).add(nz(tooling)).add(nz(packaging));
        }
        public BigDecimal primeCost() {
            return nz(material).add(nz(directLabor) != null && nz(directLabor).signum() != 0 ? nz(directLabor) : nz(process));
        }
        public BigDecimal manufacturingCost() {
            return subtotal();
        }
        public BigDecimal totalDirectCost() {
            return nz(material).add(nz(process)).add(nz(tooling)).add(nz(packaging));
        }
    }

    public record AppliedLine(
            int sequenceNo,
            String ruleCode,
            String ruleName,
            String ruleType,
            String baseCode,
            BigDecimal baseAmount,
            BigDecimal rateOrFixed,
            BigDecimal amount,
            String componentCode,
            String costBehavior,
            String calculationRule
    ) {}

    public record SheetResult(
            List<AppliedLine> lines,
            BigDecimal totalOverhead,
            BigDecimal grandTotal,
            String calculationBasis
    ) {}

    public static SheetResult apply(CostBase base, List<CostingSheetRule> rules) {
        BigDecimal running = base.subtotal();
        BigDecimal overheadTotal = BigDecimal.ZERO;
        List<AppliedLine> lines = new ArrayList<>();
        StringBuilder basis = new StringBuilder("A6 sheet: base subtotal=")
                .append(base.subtotal().setScale(SCALE, RoundingMode.HALF_UP).toPlainString());

        for (CostingSheetRule rule : rules) {
            if (!rule.isActive()) {
                continue;
            }
            BigDecimal amount;
            BigDecimal baseAmount = BigDecimal.ZERO;
            BigDecimal rateOrFixed;
            String calc;

            if (rule.getRuleType() == CostingSheetRuleType.FIXED_AMOUNT) {
                amount = nz(rule.getFixedAmount()).setScale(SCALE, RoundingMode.HALF_UP);
                rateOrFixed = amount;
                calc = "FIXED(" + amount.toPlainString() + ")";
            } else if (rule.getRuleType() == CostingSheetRuleType.PERCENT_OF_BASE) {
                baseAmount = resolveBase(base, running, rule.getBaseCode());
                BigDecimal pct = rule.getPercentRate();
                if (pct == null) {
                    throw new BusinessRuleViolationException(
                            "PERCENT_OF_BASE rule requires percent_rate: " + rule.getRuleCode(),
                            "CS_PCT_REQUIRED");
                }
                // percent_rate is fraction e.g. 0.10 = 10%, or 10 meaning 10% — support both: if > 1 treat as percent points
                BigDecimal fraction = pct.compareTo(BigDecimal.ONE) > 0
                        ? pct.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
                        : pct;
                amount = baseAmount.multiply(fraction).setScale(SCALE, RoundingMode.HALF_UP);
                rateOrFixed = fraction;
                calc = "PERCENT(" + fraction.toPlainString() + ") x base("
                        + (rule.getBaseCode() != null ? rule.getBaseCode().name() : "?")
                        + "=" + baseAmount.toPlainString() + ")";
            } else {
                throw new BusinessRuleViolationException(
                        "Unknown rule type: " + rule.getRuleType(), "CS_RULE_TYPE");
            }

            running = running.add(amount);
            overheadTotal = overheadTotal.add(amount);
            lines.add(new AppliedLine(
                    rule.getSequenceNo(),
                    rule.getRuleCode(),
                    rule.getRuleName(),
                    rule.getRuleType().name(),
                    rule.getBaseCode() != null ? rule.getBaseCode().name() : null,
                    baseAmount.setScale(SCALE, RoundingMode.HALF_UP),
                    rateOrFixed.setScale(SCALE, RoundingMode.HALF_UP),
                    amount,
                    rule.getComponentCode(),
                    rule.getCostBehavior(),
                    calc
            ));
            basis.append(" | ").append(rule.getRuleCode()).append("=").append(amount.toPlainString());
        }

        String basisStr = basis.toString();
        if (basisStr.length() > 2000) {
            basisStr = basisStr.substring(0, 2000);
        }
        return new SheetResult(
                lines,
                overheadTotal.setScale(SCALE, RoundingMode.HALF_UP),
                running.setScale(SCALE, RoundingMode.HALF_UP),
                basisStr
        );
    }

    private static BigDecimal resolveBase(CostBase base, BigDecimal running, CostingSheetBaseCode code) {
        if (code == null) {
            throw new BusinessRuleViolationException("PERCENT rule requires base_code", "CS_BASE_REQUIRED");
        }
        return switch (code) {
            case MATERIAL -> nz(base.material());
            case PROCESS -> nz(base.process());
            case TOOLING -> nz(base.tooling());
            case PACKAGING -> nz(base.packaging());
            case SUBTOTAL -> base.subtotal();
            case TOTAL_SO_FAR -> running;
            case DIRECT_LABOR -> nz(base.directLabor());
            case MACHINE -> nz(base.machine());
            case PRIME_COST -> base.primeCost();
            case MANUFACTURING_COST -> base.manufacturingCost();
            case TOTAL_DIRECT_COST -> base.totalDirectCost();
        };
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
