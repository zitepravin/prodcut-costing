package com.automotive.costing.universal.engine;

import com.automotive.costing.common.costing.CostComponentResult;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.universal.domain.CostMethodType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class UniversalCostCalculationEngine {
    public static final int MONEY_SCALE = 6;
    public static final RoundingMode RM = RoundingMode.HALF_UP;

    public record CalculationInput(
            CostMethodType method,
            List<CostComponentResult> directComponents,
            AbcAllocationEngine.AllocationResult abcAllocation,
            BigDecimal quantity,
            BigDecimal scrapFactor
    ) {}

    public record CalculationOutput(
            List<CostComponentResult> components,
            BigDecimal totalCost,
            BigDecimal unitCost,
            BigDecimal allocatedIndirect
    ) {}

    private final AbcAllocationEngine abcAllocationEngine;

    public UniversalCostCalculationEngine(AbcAllocationEngine abcAllocationEngine) {
        this.abcAllocationEngine = abcAllocationEngine;
    }

    public CalculationOutput calculate(CalculationInput input) {
        if (input.method() == null) {
            throw new BusinessRuleViolationException("Cost method is required", "UCALC-01");
        }
        List<CostComponentResult> comps = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(MONEY_SCALE, RM);
        if (input.directComponents() != null) {
            for (CostComponentResult c : input.directComponents()) {
                if (c == null || c.amount() == null) continue;
                if (c.amount().compareTo(BigDecimal.ZERO) < 0) {
                    throw new BusinessRuleViolationException("Component amount cannot be negative: " + c.componentCode(), "UCALC-02");
                }
                comps.add(c);
                total = total.add(c.amount());
            }
        }
        BigDecimal allocated = BigDecimal.ZERO.setScale(MONEY_SCALE, RM);
        if ((input.method() == CostMethodType.ABC || input.method() == CostMethodType.HYBRID)
                && input.abcAllocation() != null) {
            allocated = input.abcAllocation().totalAllocated();
            total = total.add(allocated);
        }
        if (input.scrapFactor() != null && input.scrapFactor().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal mult = BigDecimal.ONE.add(input.scrapFactor().divide(new BigDecimal("100"), 8, RM));
            total = total.multiply(mult).setScale(MONEY_SCALE, RM);
        }
        total = total.setScale(MONEY_SCALE, RM);
        BigDecimal unit = null;
        if (input.quantity() != null && input.quantity().compareTo(BigDecimal.ZERO) > 0) {
            unit = total.divide(input.quantity(), MONEY_SCALE, RM);
        }
        return new CalculationOutput(List.copyOf(comps), total, unit, allocated);
    }
}
