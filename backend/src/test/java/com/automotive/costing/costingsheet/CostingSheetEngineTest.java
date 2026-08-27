package com.automotive.costing.costingsheet;

import com.automotive.costing.costingsheet.engine.CostingSheetEngine;
import com.automotive.costing.costingsheet.engine.CostingSheetEngine.CostBase;
import com.automotive.costing.costingsheet.engine.CostingSheetEngine.SheetResult;
import com.automotive.costing.costingsheet.entity.CostingSheetBaseCode;
import com.automotive.costing.costingsheet.entity.CostingSheetRule;
import com.automotive.costing.costingsheet.entity.CostingSheetRuleType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CostingSheetEngineTest {

    @Test
    void apply_adminOnSubtotalOnly_matches152_55() {
        CostBase base = new CostBase(
                new BigDecimal("100"),
                new BigDecimal("35"),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
        CostingSheetRule factory = rule(10, "FOH", CostingSheetRuleType.PERCENT_OF_BASE,
                CostingSheetBaseCode.SUBTOTAL, new BigDecimal("0.10"), null, "FACTORY_OH");
        CostingSheetRule admin = rule(20, "AOH", CostingSheetRuleType.PERCENT_OF_BASE,
                CostingSheetBaseCode.SUBTOTAL, new BigDecimal("0.03"), null, "ADMIN_OH");

        SheetResult r = CostingSheetEngine.apply(base, List.of(factory, admin));
        assertEquals(0, new BigDecimal("13.500000").compareTo(r.lines().get(0).amount()));
        assertEquals(0, new BigDecimal("4.050000").compareTo(r.lines().get(1).amount()));
        assertEquals(0, new BigDecimal("152.550000").compareTo(r.grandTotal()));
    }

    private static CostingSheetRule rule(
            int seq, String code, CostingSheetRuleType type, CostingSheetBaseCode base,
            BigDecimal pct, BigDecimal fixed, String component) {
        CostingSheetRule r = new CostingSheetRule();
        r.setSequenceNo(seq);
        r.setRuleCode(code);
        r.setRuleName(code);
        r.setRuleType(type);
        r.setBaseCode(base);
        r.setPercentRate(pct);
        r.setFixedAmount(fixed);
        r.setComponentCode(component);
        r.setCostBehavior("FIXED");
        r.setActive(true);
        return r;
    }
}
