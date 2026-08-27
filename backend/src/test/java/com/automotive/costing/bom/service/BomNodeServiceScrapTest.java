package com.automotive.costing.bom.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies BOM-level scrap allowance composition on leaf unit costs
 * (TASKS P0: material + process + tooling + scrap%).
 */
class BomNodeServiceScrapTest {

    @Test
    void scrapNullOrZero_leavesBaseUnchanged() {
        BigDecimal base = new BigDecimal("100.000000");
        assertThat(BomNodeService.applyBomScrapAllowance(base, null))
                .isEqualByComparingTo(base);
        assertThat(BomNodeService.applyBomScrapAllowance(base, BigDecimal.ZERO))
                .isEqualByComparingTo(base);
    }

    @Test
    void scrapTenPercent_dividesByPointNine() {
        // 100 / (1 - 0.10) = 111.111111 -> scale 6 HALF_UP = 111.111111
        BigDecimal result = BomNodeService.applyBomScrapAllowance(
                new BigDecimal("100"), new BigDecimal("0.10"));
        assertThat(result).isEqualByComparingTo("111.111111");
    }

    @Test
    void scrapAtOrAboveOne_throws() {
        assertThatThrownBy(() ->
                BomNodeService.applyBomScrapAllowance(new BigDecimal("50"), BigDecimal.ONE))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("scrapPct");
        assertThatThrownBy(() ->
                BomNodeService.applyBomScrapAllowance(new BigDecimal("50"), new BigDecimal("-0.01")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
