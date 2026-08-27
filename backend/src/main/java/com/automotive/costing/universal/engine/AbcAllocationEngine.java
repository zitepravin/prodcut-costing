package com.automotive.costing.universal.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class AbcAllocationEngine {
    public static final int MONEY_SCALE = 6;
    public static final int RATE_SCALE = 8;
    public static final RoundingMode RM = RoundingMode.HALF_UP;

    public record PoolInput(String poolCode, BigDecimal poolAmount, BigDecimal totalDriverQuantity) {}
    public record UsageInput(String poolCode, BigDecimal objectDriverUsage) {}
    public record AllocationLine(String poolCode, BigDecimal driverRate, BigDecimal usage, BigDecimal allocatedCost) {}
    public record AllocationResult(List<AllocationLine> lines, BigDecimal totalAllocated) {}

    public AllocationResult allocate(List<PoolInput> pools, List<UsageInput> usages) {
        if (pools == null || pools.isEmpty()) {
            return new AllocationResult(List.of(), BigDecimal.ZERO.setScale(MONEY_SCALE, RM));
        }
        List<AllocationLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (PoolInput pool : pools) {
            if (pool.poolAmount() == null || pool.totalDriverQuantity() == null) {
                throw new BusinessRuleViolationException("Pool amount and driver quantity required", "ABC-01");
            }
            if (pool.totalDriverQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleViolationException("Driver quantity must be positive for pool " + pool.poolCode(), "ABC-02");
            }
            if (pool.poolAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleViolationException("Pool amount cannot be negative", "ABC-03");
            }
            BigDecimal rate = pool.poolAmount().divide(pool.totalDriverQuantity(), RATE_SCALE, RM);
            BigDecimal usage = usages == null ? BigDecimal.ZERO : usages.stream()
                    .filter(u -> pool.poolCode().equals(u.poolCode()))
                    .map(UsageInput::objectDriverUsage)
                    .findFirst().orElse(BigDecimal.ZERO);
            if (usage.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleViolationException("Driver usage cannot be negative", "ABC-04");
            }
            BigDecimal allocated = rate.multiply(usage).setScale(MONEY_SCALE, RM);
            lines.add(new AllocationLine(pool.poolCode(), rate, usage, allocated));
            total = total.add(allocated);
        }
        return new AllocationResult(lines, total.setScale(MONEY_SCALE, RM));
    }
}
