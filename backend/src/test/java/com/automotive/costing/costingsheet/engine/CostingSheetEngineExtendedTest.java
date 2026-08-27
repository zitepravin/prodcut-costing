package com.automotive.costing.costingsheet.engine;

import com.automotive.costing.costingsheet.entity.CostingSheetBaseCode;
import com.automotive.costing.costingsheet.entity.CostingSheetRule;
import com.automotive.costing.costingsheet.entity.CostingSheetRuleType;
import com.automotive.costing.costingsheet.engine.CostingSheetEngine.CostBase;
import com.automotive.costing.costingsheet.engine.CostingSheetEngine.SheetResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CostingSheetEngineExtendedTest {

    private CostingSheetRule percent(String code, int seq, CostingSheetBaseCode base, String pct, String component) {
        CostingSheetRule r = new CostingSheetRule();
        r.setRuleId(code);
        r.setSheetId("sheet-1");
        r.setSequenceNo(seq);
        r.setRuleCode(code);
        r.setRuleName(code);
        r.setRuleType(CostingSheetRuleType.PERCENT_OF_BASE);
        r.setBaseCode(base);
        r.setPercentRate(new BigDecimal(pct));
        r.setComponentCode(component);
        r.setActive(true);
        return r;
    }

    private CostingSheetRule fixed(String code, int seq, String amount, String component) {
        CostingSheetRule r = new CostingSheetRule();
        r.setRuleId(code);
        r.setSheetId("sheet-1");
        r.setSequenceNo(seq);
        r.setRuleCode(code);
        r.setRuleName(code);
        r.setRuleType(CostingSheetRuleType.FIXED_AMOUNT);
        r.setFixedAmount(new BigDecimal(amount));
        r.setComponentCode(component);
        r.setActive(true);
        return r;
    }

    @Test
    void percentageOnProcess() {
        CostBase base = new CostBase(new BigDecimal("100"), new BigDecimal("50"), BigDecimal.ZERO, BigDecimal.ZERO);
        SheetResult r = CostingSheetEngine.apply(base, List.of(
                percent("PROD_OH", 10, CostingSheetBaseCode.PROCESS, "0.10", "PRODUCTION_OVERHEAD")));
        assertEquals(0, new BigDecimal("5.000000").compareTo(r.totalOverhead()));
        assertEquals(0, new BigDecimal("155.000000").compareTo(r.grandTotal()));
    }

    @Test
    void orderedRules_totalSoFar() {
        CostBase base = new CostBase(new BigDecimal("100"), new BigDecimal("50"), BigDecimal.ZERO, BigDecimal.ZERO);
        SheetResult r = CostingSheetEngine.apply(base, List.of(
                percent("PROD_OH", 10, CostingSheetBaseCode.PROCESS, "0.10", "PRODUCTION_OVERHEAD"),
                percent("FACT_OH", 20, CostingSheetBaseCode.TOTAL_SO_FAR, "0.05", "FACTORY_OVERHEAD")));
        assertEquals(0, new BigDecimal("12.750000").compareTo(r.totalOverhead()));
        assertEquals(0, new BigDecimal("162.750000").compareTo(r.grandTotal()));
        assertEquals(2, r.lines().size());
    }

    @Test
    void fixedOverhead() {
        CostBase base = new CostBase(new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        SheetResult r = CostingSheetEngine.apply(base, List.of(
                fixed("ADMIN", 10, "25", "ADMIN_OVERHEAD")));
        assertEquals(0, new BigDecimal("25.000000").compareTo(r.totalOverhead()));
        assertEquals(0, new BigDecimal("35.000000").compareTo(r.grandTotal()));
    }

    @Test
    void inactiveRuleSkipped() {
        CostingSheetRule inactive = percent("X", 10, CostingSheetBaseCode.MATERIAL, "0.50", "SGA");
        inactive.setActive(false);
        CostBase base = new CostBase(new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        SheetResult r = CostingSheetEngine.apply(base, List.of(inactive));
        assertEquals(0, BigDecimal.ZERO.compareTo(r.totalOverhead()));
        assertEquals(0, new BigDecimal("100.000000").compareTo(r.grandTotal()));
    }

    @Test
    void directLaborBase() {
        CostBase base = new CostBase(new BigDecimal("100"), new BigDecimal("50"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("40"), new BigDecimal("30"));
        SheetResult r = CostingSheetEngine.apply(base, List.of(
                percent("OH", 10, CostingSheetBaseCode.DIRECT_LABOR, "0.20", "PRODUCTION_OVERHEAD")));
        assertEquals(0, new BigDecimal("8.000000").compareTo(r.totalOverhead()));
    }
}
