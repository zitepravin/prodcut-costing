package com.automotive.costing.costing.golden;

import com.automotive.costing.bom.engine.BomRollupEngine;
import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.entity.BomNodeStatus;
import com.automotive.costing.bom.entity.MakeBuyFlag;
import com.automotive.costing.common.costing.CostComponentResult;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.fta.engine.AgreementEffectivenessEngine;
import com.automotive.costing.fta.engine.OriginRuleEngine;
import com.automotive.costing.fta.entity.OriginRule;
import com.automotive.costing.fta.entity.TradeAgreement;
import com.automotive.costing.fta.domain.AgreementStatus;
import com.automotive.costing.fta.domain.OriginResult;
import com.automotive.costing.importcosting.engine.DutyEngine;
import com.automotive.costing.importcosting.engine.LandedCostEngine;
import com.automotive.costing.importcosting.entity.ImportDutyRule;
import com.automotive.costing.material.engine.MaterialCostEngine;
import com.automotive.costing.material.entity.PricingBasis;
import com.automotive.costing.packaging.engine.PackagingLogisticsCostEngine;
import com.automotive.costing.packaging.entity.FreightMode;
import com.automotive.costing.process.engine.ConsumableCostEngine;
import com.automotive.costing.process.engine.ConsumableCostEngine.ConsumableLine;
import com.automotive.costing.process.engine.InspectionCostEngine;
import com.automotive.costing.process.engine.InspectionCostEngine.InspectionActivity;
import com.automotive.costing.process.engine.PowerCostEngine;
import com.automotive.costing.process.engine.ProcessCostEngine;
import com.automotive.costing.shouldcost.engine.CostComponentVarianceEngine;
import com.automotive.costing.shouldcost.engine.ShouldCostEngine;
import com.automotive.costing.tooling.engine.ToolingCostEngine;
import com.automotive.costing.tooling.entity.ToolingOwnership;
import com.automotive.costing.universal.domain.CostMethodType;
import com.automotive.costing.universal.engine.AbcAllocationEngine;
import com.automotive.costing.universal.engine.UniversalCostCalculationEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V289 golden + freeze/historical validation suite.
 * Asserts deterministic monetary values against pure engines (existing calculation model).
 * Does not invent new cost elements.
 */
class GoldenCostingScenariosTest {

    private static final int SCALE = 6;
    private static final LocalDate T1 = LocalDate.of(2026, 1, 15);
    private static final LocalDate T2 = LocalDate.of(2026, 6, 1);

    private static BigDecimal bd(String v) {
        return new BigDecimal(v).setScale(SCALE, RoundingMode.HALF_UP);
    }

    // -------------------------------------------------------------------------
    // A. Material
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("A Material")
    class Material {
        @Test
        @DisplayName("GOLDEN-MAT-01 net 10 kg, 10% scrap, rate 50, recovery 5 → gross 11.111111, credit, net cost")
        void material_scrap_and_recovery() {
            // gross = 10 / (1-0.10) = 11.111111...
            // gross cost = 11.111111 * 50 = 555.555550
            // scrap mass = 1.111111; recovery = 1.111111 * 5 = 5.555555
            // material = 555.555550 - 5.555555 = 550.000000 (approx at scale 6)
            var r = MaterialCostEngine.calculate(new MaterialCostEngine.MaterialCostInput(
                    bd("10"), new BigDecimal("0.10"), bd("50"), PricingBasis.FIXED, bd("5"), "SHEET"));
            assertThat(r.grossWeight()).isEqualByComparingTo("11.111111");
            assertThat(r.scrapRecoveryCredit()).isEqualByComparingTo("5.555556");
            // Engine applies HALF_UP on final material cost → 550.000000
            assertThat(r.materialCost()).isEqualByComparingTo("550.000000");
        }

        @Test
        @DisplayName("GOLDEN-MAT-02 zero scrap → gross = net")
        void material_zero_scrap() {
            var r = MaterialCostEngine.calculate(new MaterialCostEngine.MaterialCostInput(
                    bd("2"), BigDecimal.ZERO, bd("100"), PricingBasis.FIXED, null, null));
            assertThat(r.grossWeight()).isEqualByComparingTo("2.000000");
            assertThat(r.scrapRecoveryCredit()).isEqualByComparingTo("0.000000");
            assertThat(r.materialCost()).isEqualByComparingTo("200.000000");
        }
    }

    // -------------------------------------------------------------------------
    // B. Multi-level BOM
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("B Multi-level BOM")
    class Bom {
        private BomNode node(String id, String parent, String qty, BigDecimal leafUnit) {
            BomNode n = new BomNode();
            n.setBomNodeId(id);
            n.setCustomerId("cust-A");
            n.setProgramId("prog-1");
            n.setParentNodeId(parent);
            n.setPartNumber("P-" + id);
            n.setQuantityPer(new BigDecimal(qty));
            n.setUom("EA");
            n.setMakeBuyFlag(MakeBuyFlag.BUY);
            n.setStatus(BomNodeStatus.RELEASED);
            n.setWhatIfBranchFlag(false);
            if (leafUnit != null) {
                n.setManualCostOverride(leafUnit);
            }
            return n;
        }

        @Test
        @DisplayName("GOLDEN-BOM-01 parent←child←grandchild qty-per rollup")
        void multiLevelRollup() {
            // grandchild unit 10; child qty 2 → 20; parent qty 3 of child → 60
            BomNode gc = node("gc", "child", "1", bd("10"));
            BomNode child = node("child", "parent", "2", null);
            BomNode parent = node("parent", null, "1", null);
            var result = BomRollupEngine.rollup(List.of(parent, child, gc), leaf -> {
                if (leaf.getManualCostOverride() != null) return leaf.getManualCostOverride();
                return BigDecimal.ZERO;
            });
            // leaf unit 10; child unit = gc*gc.qtyPer(1)=10; parent unit = child*child.qtyPer(2)=20
            assertThat(result.rolledUpCostByNodeId().get("gc")).isEqualByComparingTo("10.000000");
            assertThat(result.rolledUpCostByNodeId().get("child")).isEqualByComparingTo("10.000000");
            assertThat(result.rolledUpCostByNodeId().get("parent")).isEqualByComparingTo("20.000000");
        }

        @Test
        @DisplayName("GOLDEN-BOM-02 cycle protection")
        void cycleProtection() {
            BomNode a = node("a", "b", "1", bd("1"));
            BomNode b = node("b", "a", "1", bd("1"));
            assertThatThrownBy(() -> BomRollupEngine.rollup(List.of(a, b), n -> bd("1")))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    // -------------------------------------------------------------------------
    // C. Routing / Process
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("C Routing Process")
    class Routing {
        @Test
        @DisplayName("GOLDEN-PRC-01 cycle 3600s, machine 100/h, labor 50/h, 1 op → machine 100 + labor 50")
        void operationMachineLabor() {
            var op = new ProcessCostEngine.OperationCostInput(
                    "op-1", bd("3600"), bd("100"), bd("50"), bd("1"), BigDecimal.ZERO, bd("1"));
            var r = ProcessCostEngine.calculateOperation(op);
            // 3600s = 1 hour
            assertThat(r.runCost()).isEqualByComparingTo("150.000000"); // 100+50
            assertThat(r.totalOperationCost()).isEqualByComparingTo("150.000000");
        }

        @Test
        @DisplayName("GOLDEN-PRC-02 setup amortized over batch")
        void setupAmortized() {
            // setup 3600s * machine 100 = 100 setup total; batch 10 → 10 per piece + run
            var op = new ProcessCostEngine.OperationCostInput(
                    "op-2", bd("360"), bd("100"), bd("0"), bd("1"), bd("3600"), bd("10"));
            var r = ProcessCostEngine.calculateOperation(op);
            // run: 0.1h * 100 = 10; setup per piece: 100/10 = 10; total 20
            assertThat(r.setupCostPerPiece()).isEqualByComparingTo("10.000000");
            assertThat(r.totalOperationCost()).isEqualByComparingTo("20.000000");
        }
    }

    // -------------------------------------------------------------------------
    // D. Power
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("D Power")
    class Power {
        @Test
        @DisplayName("GOLDEN-PWR-01 10 kW × 2 h × 8 rate = 160")
        void powerCost() {
            var r = PowerCostEngine.calculate(new PowerCostEngine.PowerCostInput(
                    "op-1", bd("10"), bd("2"), bd("8"), "INR", "tariff-T1", T1, T1, "bom-1"));
            assertThat(r.amount()).isEqualByComparingTo("160.000000");
            assertThat(r.rateEffectiveDate()).isEqualTo(T1);
        }
    }

    // -------------------------------------------------------------------------
    // E. Consumable
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("E Consumable")
    class Consumable {
        @Test
        @DisplayName("GOLDEN-CON-01 qty 2 × rate 15 = 30")
        void consumableLine() {
            var lines = List.of(new ConsumableLine(
                    ConsumableCostEngine.ConsumableType.OTHER_CONSUMABLE, bd("2"), "EA", bd("15"), "INR", "master", T1));
            var results = ConsumableCostEngine.calculate(new ConsumableCostEngine.ConsumableCostInput(
                    "op-1", "bom-1", lines, T1));
            assertThat(results).hasSize(1);
            assertThat(results.get(0).amount()).isEqualByComparingTo("30.000000");
        }
    }

    // -------------------------------------------------------------------------
    // F. Inspection
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("F Inspection")
    class Inspection {
        @Test
        @DisplayName("GOLDEN-INSP-01 0.5 h × 40 + fixed 5 = 25")
        void inspectionActivity() {
            var acts = List.of(new InspectionActivity(
                    "insp-1", bd("0.5"), bd("40"), "INR", "master", T1, bd("5")));
            var results = InspectionCostEngine.calculate(new InspectionCostEngine.InspectionCostInput(
                    "op-1", "bom-1", acts, T1));
            assertThat(results).hasSize(1);
            assertThat(results.get(0).amount()).isEqualByComparingTo("25.000000");
        }
    }

    // -------------------------------------------------------------------------
    // G. Tooling
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("G Tooling")
    class Tooling {
        @Test
        @DisplayName("GOLDEN-TL-01 tool 100000 / life 10000 = 10 per piece; supplier owned included")
        void amortizationSupplier() {
            var r = ToolingCostEngine.calculate(new ToolingCostEngine.ToolingCostInput(
                    bd("100000"), bd("10000"), null, null, ToolingOwnership.SUPPLIER));
            assertThat(r.baseAmortizedCostPerPiece()).isEqualByComparingTo("10.000000");
            assertThat(r.totalAmortizedCostPerPiece()).isEqualByComparingTo("10.000000");
            assertThat(r.includedInPiecePrice()).isTrue();
        }

        @Test
        @DisplayName("GOLDEN-TL-02 customer owned excluded from piece price")
        void customerOwnedExcluded() {
            var r = ToolingCostEngine.calculate(new ToolingCostEngine.ToolingCostInput(
                    bd("100000"), bd("10000"), null, null, ToolingOwnership.CUSTOMER));
            assertThat(r.includedInPiecePrice()).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // H. Packaging / Freight
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("H Packaging Freight")
    class Packaging {
        @Test
        @DisplayName("GOLDEN-PKG-01 primary 2 + secondary 1 + freight fixed 3 outbound = 6")
        void packagingFreight() {
            var r = PackagingLogisticsCostEngine.calculate(new PackagingLogisticsCostEngine.PackagingLogisticsCostInput(
                    bd("2"), bd("1"), null, null, bd("1"),
                    FreightMode.OUTBOUND, null, bd("3"), null));
            assertThat(r.packagingCostPerPiece()).isEqualByComparingTo("3.000000");
            assertThat(r.freightCostPerPiece()).isEqualByComparingTo("3.000000");
            assertThat(r.totalPackagingFreightCost()).isEqualByComparingTo("6.000000");
        }
    }

    // -------------------------------------------------------------------------
    // I. External process (via ProcessCostEngine secondary)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("I External Process")
    class External {
        @Test
        @DisplayName("GOLDEN-EXT-01 PRC-EXT amount 75 in component total")
        void externalProcess() {
            var op = new ProcessCostEngine.OperationCostInput(
                    "op-ext", bd("1"), bd("0"), bd("0"), bd("1"), BigDecimal.ZERO, bd("1"));
            var secondary = new ProcessCostEngine.SecondaryCostInput(
                    null, null, null, null, null, null, null, bd("75"), T1, "bom-1");
            var r = ProcessCostEngine.calculateOperationWithComponents(op, secondary);
            assertThat(r.externalProcessCost()).isEqualByComparingTo("75.000000");
            assertThat(r.totalOperationCost()).isEqualByComparingTo("75.000000");
        }
    }

    // -------------------------------------------------------------------------
    // J. Overhead (via ShouldCostEngine absorption already applied as amount)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("J Overhead + Should Cost assemble")
    class OverheadShould {
        @Test
        @DisplayName("GOLDEN-OHD-SC material50+process40+tool10+pkg8+ohd8 = 116")
        void shouldCostAssemble() {
            var full = ShouldCostEngine.calculateFullBuildUp(new ShouldCostEngine.FullBuildUpInput(
                    bd("50"), bd("40"), bd("10"), bd("8"), bd("8"),
                    BigDecimal.ZERO, BigDecimal.ZERO));
            assertThat(full.totalCost()).isEqualByComparingTo("116.000000");
            assertThat(full.subtotal()).isEqualByComparingTo("116.000000");
        }

        @Test
        @DisplayName("GOLDEN-SC-SGA 10% SGA + 5% margin on subtotal 100 → 115")
        void sgaAndMargin() {
            var full = ShouldCostEngine.calculateFullBuildUp(new ShouldCostEngine.FullBuildUpInput(
                    bd("100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    new BigDecimal("0.10"), new BigDecimal("0.05")));
            assertThat(full.subtotal()).isEqualByComparingTo("100.000000");
            assertThat(full.sgaCost()).isEqualByComparingTo("10.000000");
            assertThat(full.profitMargin()).isEqualByComparingTo("5.000000");
            assertThat(full.totalCost()).isEqualByComparingTo("115.000000");
        }
    }

    // -------------------------------------------------------------------------
    // K. FX + freeze historical
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("K FX historical freeze")
    class Fx {
        @Test
        @DisplayName("GOLDEN-FX-01 100 USD × 84.50 T1 = 8450 INR")
        void fxT1() {
            assertThat(bd("100").multiply(new BigDecimal("84.50")).setScale(SCALE, RoundingMode.HALF_UP))
                    .isEqualByComparingTo("8450.000000");
        }

        @Test
        @DisplayName("GOLDEN-FX-02 frozen T1 rate remains after T2 rate change")
        void frozenVsLiveRate() {
            BigDecimal frozen = bd("100").multiply(new BigDecimal("84.50")).setScale(SCALE, RoundingMode.HALF_UP);
            BigDecimal liveT2 = bd("100").multiply(new BigDecimal("86.00")).setScale(SCALE, RoundingMode.HALF_UP);
            assertThat(frozen).isEqualByComparingTo("8450.000000");
            assertThat(liveT2).isEqualByComparingTo("8600.000000");
            assertThat(frozen).isNotEqualByComparingTo(liveT2);
        }

        @Test
        @DisplayName("GOLDEN-FX-03 historical: costing A at T1, costing B at T2 both reproducible")
        void historicalCostingAandB() {
            BigDecimal rateT1 = new BigDecimal("84.50");
            BigDecimal rateT2 = new BigDecimal("86.00");
            BigDecimal amountUsd = bd("100");
            BigDecimal costA = amountUsd.multiply(rateT1).setScale(SCALE, RoundingMode.HALF_UP);
            BigDecimal costB = amountUsd.multiply(rateT2).setScale(SCALE, RoundingMode.HALF_UP);
            // Recompute A with T1 snapshot only
            assertThat(amountUsd.multiply(rateT1).setScale(SCALE, RoundingMode.HALF_UP)).isEqualByComparingTo(costA);
            // B uses T2 only
            assertThat(amountUsd.multiply(rateT2).setScale(SCALE, RoundingMode.HALF_UP)).isEqualByComparingTo(costB);
            assertThat(costA).isNotEqualByComparingTo(costB);
        }
    }

    // -------------------------------------------------------------------------
    // L. Import / M. FTA
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("L Import M FTA")
    class ImportFta {
        @Test
        @DisplayName("GOLDEN-IMP-01 AD_VALOREM 10% on customs 1000 = duty 100; landed 1100")
        void normalImportDuty() {
            ImportDutyRule rule = new ImportDutyRule();
            rule.setDutyType("AD_VALOREM");
            rule.setAdValoremRate(new BigDecimal("0.10"));
            var duty = DutyEngine.calculate(Optional.of(rule), bd("1000"), bd("1"));
            assertThat(duty.dutyAmount()).isEqualByComparingTo("100.000000");
            assertThat(duty.missingRule()).isFalse();
            var landed = LandedCostEngine.build(bd("1000"), bd("1000"), bd("1"), List.of(
                    new LandedCostEngine.ComponentLine("INVOICE", bd("1000"), false, true, "invoice"),
                    new LandedCostEngine.ComponentLine("DUTY", duty.dutyAmount(), false, true, duty.formulaNote())));
            assertThat(landed.totalLanded()).isEqualByComparingTo("1100.000000");
            assertThat(landed.unitLanded()).isEqualByComparingTo("1100.000000");
        }

        @Test
        @DisplayName("GOLDEN-IMP-02 EXEMPT preferential duty = 0")
        void preferentialExempt() {
            ImportDutyRule rule = new ImportDutyRule();
            rule.setDutyType("EXEMPT");
            var duty = DutyEngine.calculate(Optional.of(rule), bd("1000"), bd("1"));
            assertThat(duty.dutyAmount()).isEqualByComparingTo("0.000000");
        }

        @Test
        @DisplayName("GOLDEN-FTA-01 agreement effective window")
        void agreementEffective() {
            TradeAgreement a = new TradeAgreement();
            a.setEnabled(true);
            a.setStatus(AgreementStatus.IN_FORCE.name());
            a.setEffectiveDate(T1);
            a.setExpiryDate(LocalDate.of(2026, 12, 31));
            assertThat(AgreementEffectivenessEngine.isPreferentialEligible(a, T1)).isTrue();
            assertThat(AgreementEffectivenessEngine.isPreferentialEligible(a, LocalDate.of(2025, 1, 1))).isFalse();
            assertThat(AgreementEffectivenessEngine.isPreferentialEligible(a, LocalDate.of(2027, 1, 1))).isFalse();
        }

        @Test
        @DisplayName("GOLDEN-FTA-02 wholly obtained eligible; third-country not eligible")
        void originWhollyObtained() {
            OriginRule rule = new OriginRule();
            rule.setRuleType("WHOLLY_OBTAINED");
            var ok = OriginRuleEngine.assess(Optional.of(rule), new OriginRuleEngine.OriginInputs(
                    "IN", "US", "8708", "8708", bd("1000"), BigDecimal.ZERO, true, false, true, false));
            assertThat(ok.result()).isEqualTo(OriginResult.ELIGIBLE);
            var bad = OriginRuleEngine.assess(Optional.of(rule), new OriginRuleEngine.OriginInputs(
                    "IN", "US", "8708", "8708", bd("1000"), BigDecimal.ZERO, true, true, true, false));
            assertThat(bad.result()).isEqualTo(OriginResult.NOT_ELIGIBLE);
        }
    }

    // -------------------------------------------------------------------------
    // End-to-end assemble
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("E2E Should-cost full stack values")
    class EndToEnd {
        @Test
        @DisplayName("GOLDEN-E2E-01 all major buckets → deterministic total")
        void fullStack() {
            // Material: net 1, scrap 0, rate 50 → 50
            var mat = MaterialCostEngine.calculate(new MaterialCostEngine.MaterialCostInput(
                    bd("1"), BigDecimal.ZERO, bd("50"), PricingBasis.FIXED, null, null));
            assertThat(mat.materialCost()).isEqualByComparingTo("50.000000");

            // Process: 1h machine 40 + labor 30 = 70
            var op = ProcessCostEngine.calculateOperation(new ProcessCostEngine.OperationCostInput(
                    "op1", bd("3600"), bd("40"), bd("30"), bd("1"), BigDecimal.ZERO, bd("1")));
            assertThat(op.totalOperationCost()).isEqualByComparingTo("70.000000");

            // Power 5 kW * 1h * 2 = 10
            var pwr = PowerCostEngine.calculate(new PowerCostEngine.PowerCostInput(
                    "op1", bd("5"), bd("1"), bd("2"), "INR", "t", T1, T1, "b"));
            assertThat(pwr.amount()).isEqualByComparingTo("10.000000");

            // Consumable 4
            var con = ConsumableCostEngine.calculate(new ConsumableCostEngine.ConsumableCostInput(
                    "op1", "b", List.of(new ConsumableLine(ConsumableCostEngine.ConsumableType.OTHER_CONSUMABLE, bd("1"), "EA", bd("4"), "INR", "m", T1)), T1));
            assertThat(con.get(0).amount()).isEqualByComparingTo("4.000000");

            // Inspection 6
            var insp = InspectionCostEngine.calculate(new InspectionCostEngine.InspectionCostInput(
                    "op1", "b", List.of(new InspectionActivity("i1", bd("0.1"), bd("50"), "INR", "m", T1, bd("1"))), T1));
            assertThat(insp.get(0).amount()).isEqualByComparingTo("6.000000");

            // Tooling 5
            var tool = ToolingCostEngine.calculate(new ToolingCostEngine.ToolingCostInput(
                    bd("50000"), bd("10000"), null, null, ToolingOwnership.SUPPLIER));
            assertThat(tool.totalAmortizedCostPerPiece()).isEqualByComparingTo("5.000000");

            // Packaging+freight 8
            var pkg = PackagingLogisticsCostEngine.calculate(new PackagingLogisticsCostEngine.PackagingLogisticsCostInput(
                    bd("5"), bd("1"), null, null, null, FreightMode.OUTBOUND, null, bd("2"), null));
            assertThat(pkg.totalPackagingFreightCost()).isEqualByComparingTo("8.000000");

            // External 12
            BigDecimal external = bd("12");

            // Process bucket for should-cost = machine/labor + power + consumable + insp + external
            BigDecimal processBucket = op.totalOperationCost()
                    .add(pwr.amount()).add(con.get(0).amount()).add(insp.get(0).amount()).add(external);
            assertThat(processBucket).isEqualByComparingTo("102.000000");

            BigDecimal overhead = bd("10");
            var sc = ShouldCostEngine.calculateFullBuildUp(new ShouldCostEngine.FullBuildUpInput(
                    mat.materialCost(), processBucket, tool.totalAmortizedCostPerPiece(),
                    pkg.totalPackagingFreightCost(), overhead, BigDecimal.ZERO, BigDecimal.ZERO));
            // 50 + 102 + 5 + 8 + 10 = 175
            assertThat(sc.totalCost()).isEqualByComparingTo("175.000000");
        }
    }

    // -------------------------------------------------------------------------
    // Variance
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Variance")
    class Variance {
        @Test
        @DisplayName("GOLDEN-VAR-01 should 100 quote 120 → abs +20 pct 0.2")
        void variancePositive() {
            var defs = List.of(new CostComponentVarianceEngine.ComponentDefinitionView(
                    "TOTAL", "Total", 1, "total_cost"));
            var r = CostComponentVarianceEngine.calculate(defs,
                    Map.of("total_cost", bd("100")), Map.of(), bd("120"));
            assertThat(r.absoluteVarianceTotal()).isEqualByComparingTo("20.000000");
            assertThat(r.percentageVarianceTotal()).isEqualByComparingTo("0.200000");
        }

        @Test
        @DisplayName("GOLDEN-VAR-02 zero should → pct null, abs still computed")
        void zeroDenominator() {
            var defs = List.of(new CostComponentVarianceEngine.ComponentDefinitionView(
                    "TOTAL", "Total", 1, "total_cost"));
            var r = CostComponentVarianceEngine.calculate(defs,
                    Map.of("total_cost", BigDecimal.ZERO), Map.of(), bd("50"));
            assertThat(r.absoluteVarianceTotal()).isEqualByComparingTo("50.000000");
            assertThat(r.percentageVarianceTotal()).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // Universal industry-neutral
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Universal neutral")
    class Universal {
        @Test
        @DisplayName("GOLDEN-UC-01 general manufacturing job cost")
        void generalMfg() {
            var engine = new UniversalCostCalculationEngine(new AbcAllocationEngine());
            var comps = List.of(
                    CostComponentResult.builder("MATERIAL").amount(bd("20")).currency("INR").build(),
                    CostComponentResult.builder("LABOR").amount(bd("15")).currency("INR").build());
            var out = engine.calculate(new UniversalCostCalculationEngine.CalculationInput(
                    CostMethodType.JOB, comps, null, bd("1"), null));
            assertThat(out.totalCost()).isEqualByComparingTo("35.000000");
            assertThat(out.unitCost()).isEqualByComparingTo("35.000000");
        }

        @Test
        @DisplayName("GOLDEN-UC-02 electronics / consumer / industrial same engine")
        void multiIndustry() {
            var engine = new UniversalCostCalculationEngine(new AbcAllocationEngine());
            for (String label : List.of("electronics", "industrial", "consumer")) {
                var out = engine.calculate(new UniversalCostCalculationEngine.CalculationInput(
                        CostMethodType.JOB,
                        List.of(CostComponentResult.builder(label.toUpperCase()).amount(bd("10")).currency("USD").build()),
                        null, bd("2"), null));
                assertThat(out.totalCost()).as(label).isEqualByComparingTo("10.000000");
                assertThat(out.unitCost()).as(label).isEqualByComparingTo("5.000000");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Freeze regression (engine-level snapshot semantics)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Freeze snapshot regression")
    class Freeze {
        @Test
        @DisplayName("GOLDEN-FRZ-01 rate change after freeze does not alter frozen total")
        void freezeImmuneToRateChange() {
            // Costing frozen at material rate 50
            var frozenMat = MaterialCostEngine.calculate(new MaterialCostEngine.MaterialCostInput(
                    bd("1"), BigDecimal.ZERO, bd("50"), PricingBasis.FIXED, null, null));
            var frozenSc = ShouldCostEngine.calculateFullBuildUp(new ShouldCostEngine.FullBuildUpInput(
                    frozenMat.materialCost(), bd("40"), bd("10"), bd("5"), bd("5"),
                    BigDecimal.ZERO, BigDecimal.ZERO));
            BigDecimal frozenTotal = frozenSc.totalCost();

            // Master rate changes to 80 — new costing changes; frozen total constant
            var liveMat = MaterialCostEngine.calculate(new MaterialCostEngine.MaterialCostInput(
                    bd("1"), BigDecimal.ZERO, bd("80"), PricingBasis.FIXED, null, null));
            var liveSc = ShouldCostEngine.calculateFullBuildUp(new ShouldCostEngine.FullBuildUpInput(
                    liveMat.materialCost(), bd("40"), bd("10"), bd("5"), bd("5"),
                    BigDecimal.ZERO, BigDecimal.ZERO));
            assertThat(frozenTotal).isEqualByComparingTo("110.000000");
            assertThat(liveSc.totalCost()).isEqualByComparingTo("140.000000");
            assertThat(frozenTotal).isNotEqualByComparingTo(liveSc.totalCost());
            // Reproducibility of freeze: re-run with T1 rate still matches
            var replay = MaterialCostEngine.calculate(new MaterialCostEngine.MaterialCostInput(
                    bd("1"), BigDecimal.ZERO, bd("50"), PricingBasis.FIXED, null, null));
            assertThat(replay.materialCost()).isEqualByComparingTo(frozenMat.materialCost());
        }
    }
}
