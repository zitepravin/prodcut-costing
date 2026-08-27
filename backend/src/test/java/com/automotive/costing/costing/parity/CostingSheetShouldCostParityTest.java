package com.automotive.costing.costing.parity;

import com.automotive.costing.costingsheet.engine.CostingSheetEngine;
import com.automotive.costing.costingsheet.engine.CostingSheetEngine.CostBase;
import com.automotive.costing.costingsheet.engine.CostingSheetEngine.SheetResult;
import com.automotive.costing.costingsheet.entity.CostingSheetBaseCode;
import com.automotive.costing.costingsheet.entity.CostingSheetRule;
import com.automotive.costing.costingsheet.entity.CostingSheetRuleType;
import com.automotive.costing.shouldcost.engine.ShouldCostEngine;
import com.automotive.costing.shouldcost.engine.ShouldCostEngine.FullBuildUpInput;
import com.automotive.costing.shouldcost.engine.ShouldCostEngine.FullBuildUpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V290: dual-assert CostingSheetEngine grand total vs ShouldCostEngine total
 * for equivalent business inputs. Tolerance = exact BigDecimal compare (scale 6 HALF_UP).
 *
 * Mapping convention (documented):
 * - CostBase.material ↔ materialCost
 * - CostBase.process ↔ processCost
 * - CostBase.tooling ↔ toolingCost
 * - CostBase.packaging ↔ packagingFreightCost
 * - FIXED_AMOUNT / PERCENT rules on sheet ↔ overheadCost and/or sgaPct on should-cost
 * - ShouldCost marginPct is extra commercial layer; parity cases set margin=0 unless noted.
 */
class CostingSheetShouldCostParityTest {

    private static final int SCALE = 6;

    private static BigDecimal bd(String v) {
        return new BigDecimal(v).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static void assertParity(String scenario, SheetResult sheet, FullBuildUpResult should) {
        BigDecimal sheetTotal = sheet.grandTotal().setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal shouldTotal = should.totalCost().setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal diff = sheetTotal.subtract(shouldTotal).abs();
        assertThat(diff)
                .as("scenario=%s sheet=%s should=%s diff=%s currency=program-scale6 tolerance=0",
                        scenario, sheetTotal.toPlainString(), shouldTotal.toPlainString(), diff.toPlainString())
                .isEqualByComparingTo(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP));
    }

    private static CostingSheetRule fixed(String code, int seq, BigDecimal amount) {
        CostingSheetRule r = new CostingSheetRule();
        r.setRuleId("r-" + code);
        r.setSheetId("sheet-parity");
        r.setSequenceNo(seq);
        r.setRuleCode(code);
        r.setRuleName(code);
        r.setRuleType(CostingSheetRuleType.FIXED_AMOUNT);
        r.setFixedAmount(amount);
        r.setComponentCode(code);
        r.setCostBehavior("FIXED");
        r.setActive(true);
        return r;
    }

    private static CostingSheetRule percent(String code, int seq, CostingSheetBaseCode base, BigDecimal fraction) {
        CostingSheetRule r = new CostingSheetRule();
        r.setRuleId("r-" + code);
        r.setSheetId("sheet-parity");
        r.setSequenceNo(seq);
        r.setRuleCode(code);
        r.setRuleName(code);
        r.setRuleType(CostingSheetRuleType.PERCENT_OF_BASE);
        r.setBaseCode(base);
        r.setPercentRate(fraction);
        r.setComponentCode(code);
        r.setCostBehavior("VARIABLE");
        r.setActive(true);
        return r;
    }

    @Test
    @DisplayName("PARITY-01 zero rules: sheet subtotal = should-cost (ohd=0,sga=0,margin=0)")
    void parity_noRules_matchesSubtotal() {
        CostBase base = new CostBase(bd("50"), bd("40"), bd("10"), bd("8"));
        SheetResult sheet = CostingSheetEngine.apply(base, List.of());
        FullBuildUpResult should = ShouldCostEngine.calculateFullBuildUp(new FullBuildUpInput(
                bd("50"), bd("40"), bd("10"), bd("8"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        assertParity("PARITY-01", sheet, should);
        assertThat(sheet.grandTotal()).isEqualByComparingTo("108.000000");
        assertThat(should.totalCost()).isEqualByComparingTo("108.000000");
    }

    @Test
    @DisplayName("PARITY-02 fixed overhead rule = should-cost overhead amount")
    void parity_fixedOverhead() {
        CostBase base = new CostBase(bd("50"), bd("40"), bd("10"), bd("8"));
        SheetResult sheet = CostingSheetEngine.apply(base, List.of(fixed("OHD", 1, bd("12"))));
        FullBuildUpResult should = ShouldCostEngine.calculateFullBuildUp(new FullBuildUpInput(
                bd("50"), bd("40"), bd("10"), bd("8"), bd("12"), BigDecimal.ZERO, BigDecimal.ZERO));
        assertParity("PARITY-02", sheet, should);
        assertThat(sheet.grandTotal()).isEqualByComparingTo("120.000000");
    }

    @Test
    @DisplayName("PARITY-03 percent of SUBTOTAL as SGA = should-cost sgaPct")
    void parity_sgaPercent() {
        CostBase base = new CostBase(bd("100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        // 10% of SUBTOTAL
        SheetResult sheet = CostingSheetEngine.apply(base, List.of(
                percent("SGA", 1, CostingSheetBaseCode.SUBTOTAL, new BigDecimal("0.10"))));
        FullBuildUpResult should = ShouldCostEngine.calculateFullBuildUp(new FullBuildUpInput(
                bd("100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("0.10"), BigDecimal.ZERO));
        assertParity("PARITY-03", sheet, should);
        assertThat(sheet.grandTotal()).isEqualByComparingTo("110.000000");
        assertThat(should.sgaCost()).isEqualByComparingTo("10.000000");
    }

    @Test
    @DisplayName("PARITY-04 V289 E2E buckets without margin: sheet fixed OHD matches should")
    void parity_e2eBuckets() {
        // Aligns with V289 GOLDEN-E2E-01 structure: mat50 + process102 + tool5 + pkg8 + ohd10 = 175
        CostBase base = new CostBase(bd("50"), bd("102"), bd("5"), bd("8"));
        SheetResult sheet = CostingSheetEngine.apply(base, List.of(fixed("OHD", 1, bd("10"))));
        FullBuildUpResult should = ShouldCostEngine.calculateFullBuildUp(new FullBuildUpInput(
                bd("50"), bd("102"), bd("5"), bd("8"), bd("10"), BigDecimal.ZERO, BigDecimal.ZERO));
        assertParity("PARITY-04-E2E", sheet, should);
        assertThat(should.totalCost()).isEqualByComparingTo("175.000000");
    }

    @Test
    @DisplayName("PARITY-05 V289 GOLDEN A domestic 116 with ohd in should vs sheet fixed")
    void parity_goldenA() {
        CostBase base = new CostBase(bd("50"), bd("40"), bd("10"), bd("8"));
        SheetResult sheet = CostingSheetEngine.apply(base, List.of(fixed("OHD", 1, bd("8"))));
        FullBuildUpResult should = ShouldCostEngine.calculateFullBuildUp(new FullBuildUpInput(
                bd("50"), bd("40"), bd("10"), bd("8"), bd("8"), BigDecimal.ZERO, BigDecimal.ZERO));
        assertParity("PARITY-05-GOLDEN-A", sheet, should);
        assertThat(should.totalCost()).isEqualByComparingTo("116.000000");
    }

    @ParameterizedTest(name = "PARITY-PARAM material={0} process={1} tooling={2} pkg={3} ohd={4}")
    @CsvSource({
            "10,20,5,2,3",
            "0,100,0,0,0",
            "1.5,2.25,0.5,0.25,1"
    })
    void parity_parameterizedExact(String m, String p, String t, String pkg, String ohd) {
        CostBase base = new CostBase(bd(m), bd(p), bd(t), bd(pkg));
        SheetResult sheet = CostingSheetEngine.apply(base, List.of(fixed("OHD", 1, bd(ohd))));
        FullBuildUpResult should = ShouldCostEngine.calculateFullBuildUp(new FullBuildUpInput(
                bd(m), bd(p), bd(t), bd(pkg), bd(ohd), BigDecimal.ZERO, BigDecimal.ZERO));
        assertParity("PARITY-PARAM-" + m + "-" + p, sheet, should);
    }

    @Test
    @DisplayName("PARITY-06 intentional difference: should-cost margin is NOT in sheet engine")
    void intentionalDifference_marginOnlyOnShouldCost() {
        CostBase base = new CostBase(bd("100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        SheetResult sheet = CostingSheetEngine.apply(base, List.of());
        FullBuildUpResult shouldWithMargin = ShouldCostEngine.calculateFullBuildUp(new FullBuildUpInput(
                bd("100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("0.05")));
        // Documented intentional difference: margin is commercial should-cost layer, not A6 sheet rule
        assertThat(sheet.grandTotal()).isEqualByComparingTo("100.000000");
        assertThat(shouldWithMargin.totalCost()).isEqualByComparingTo("105.000000");
        assertThat(sheet.grandTotal()).isNotEqualByComparingTo(shouldWithMargin.totalCost());
    }

    @Test
    @DisplayName("PARITY-07 scrap effect via precomputed material bucket (shared input)")
    void parity_scrapAffectsSharedMaterialBucket() {
        // Material engine already applied scrap externally; both engines receive same material amount
        BigDecimal materialAfterScrap = bd("550.000000"); // V289 MAT-01 result
        CostBase base = new CostBase(materialAfterScrap, bd("0"), bd("0"), bd("0"));
        SheetResult sheet = CostingSheetEngine.apply(base, List.of());
        FullBuildUpResult should = ShouldCostEngine.calculateFullBuildUp(new FullBuildUpInput(
                materialAfterScrap, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        assertParity("PARITY-07-SCRAP-BUCKET", sheet, should);
    }
}
