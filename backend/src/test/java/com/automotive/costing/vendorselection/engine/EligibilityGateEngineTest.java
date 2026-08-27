package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.rfq.entity.SupplierMaster;
import com.automotive.costing.rfq.entity.SupplierStatus;
import com.automotive.costing.vendorselection.entity.CapabilityStatus;
import com.automotive.costing.vendorselection.entity.SupplierCapability;
import com.automotive.costing.vendorselection.engine.EligibilityGateEngine.EvaluationInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EligibilityGateEngineTest {

    private final EligibilityGateEngine engine = new EligibilityGateEngine();

    private static EvaluationInput base(
            SupplierMaster s, List<SupplierCapability> caps,
            List<String> codes, BigDecimal minCap, boolean cert,
            List<String> verifiedPolicies, List<String> requiredPolicies,
            boolean requireQ, Boolean qEligible,
            boolean noCritical, boolean noHigh, int criticalCount, int highCount,
            String lifecycleStatus) {
        return new EvaluationInput(
                s, caps, codes, minCap, cert, verifiedPolicies, requiredPolicies,
                requireQ, qEligible, noCritical, noHigh, criticalCount, highCount,
                lifecycleStatus, null);
    }

    private static SupplierMaster active() {
        SupplierMaster s = new SupplierMaster();
        s.setStatus(SupplierStatus.ACTIVE);
        return s;
    }

    @Test
    void activeSupplierWithRequiredCapability_passes() {
        SupplierCapability cap = new SupplierCapability();
        cap.setCapabilityCode("STAMPING");
        cap.setStatus(CapabilityStatus.ACTIVE);
        cap.setCertified(true);
        cap.setMaxAnnualCapacity(new BigDecimal("100000"));
        var out = engine.evaluate(base(active(), List.of(cap), List.of("STAMPING"), new BigDecimal("50000"), true,
                List.of(), List.of(), false, null, false, false, 0, 0, null));
        assertThat(out.overallEligible()).isTrue();
    }

    @Test
    void inactiveSupplier_failsStatusGate() {
        SupplierMaster s = new SupplierMaster();
        s.setStatus(SupplierStatus.INACTIVE);
        var out = engine.evaluate(base(s, List.of(), List.of(), null, false, List.of(), List.of(), false, null, false, false, 0, 0, null));
        assertThat(out.failedGates()).contains("SUPPLIER_STATUS");
    }

    @Test
    void missingCapability_fails() {
        var out = engine.evaluate(base(active(), List.of(), List.of("CNC_MILLING"), null, false,
                List.of(), List.of(), false, null, false, false, 0, 0, null));
        assertThat(out.failedGates()).contains("PART_PROCESS_CAPABILITY");
    }

    @Test
    void insufficientCapacity_fails() {
        SupplierCapability cap = new SupplierCapability();
        cap.setCapabilityCode("X");
        cap.setStatus(CapabilityStatus.ACTIVE);
        cap.setMaxAnnualCapacity(new BigDecimal("10"));
        var out = engine.evaluate(base(active(), List.of(cap), List.of(), new BigDecimal("100"), false,
                List.of(), List.of(), false, null, false, false, 0, 0, null));
        assertThat(out.failedGates()).contains("CAPACITY");
    }

    @Test
    void missingRequiredPolicy_failsPolicyGate() {
        var out = engine.evaluate(base(active(), List.of(), List.of(), null, false,
                List.of("QUALITY_POLICY"), List.of("QUALITY_POLICY", "ENVIRONMENTAL_POLICY"),
                false, null, false, false, 0, 0, null));
        assertThat(out.failedGates()).contains("POLICY_COMPLIANCE");
    }

    @Test
    void verifiedRequiredPolicies_passPolicyGate() {
        var out = engine.evaluate(base(active(), List.of(), List.of(), null, false,
                List.of("QUALITY_POLICY", "ENVIRONMENTAL_POLICY"),
                List.of("QUALITY_POLICY", "ENVIRONMENTAL_POLICY"),
                false, null, false, false, 0, 0, null));
        assertThat(out.failedGates()).doesNotContain("POLICY_COMPLIANCE");
        assertThat(out.overallEligible()).isTrue();
    }

    @Test
    void requireQuestionnaireWithoutSnapshot_fails() {
        var out = engine.evaluate(base(active(), List.of(), List.of(), null, false, List.of(), List.of(), true, null, false, false, 0, 0, null));
        assertThat(out.failedGates()).contains("QUESTIONNAIRE");
    }

    @Test
    void requireQuestionnaireWithEligibleEval_passes() {
        var out = engine.evaluate(base(active(), List.of(), List.of(), null, false, List.of(), List.of(), true, true, false, false, 0, 0, null));
        assertThat(out.failedGates()).doesNotContain("QUESTIONNAIRE");
        assertThat(out.overallEligible()).isTrue();
    }

    @Test
    void criticalOpenRisk_blocksWhenRequired() {
        var out = engine.evaluate(base(active(), List.of(), List.of(), null, false, List.of(), List.of(),
                false, null, true, false, 2, 0, null));
        assertThat(out.failedGates()).contains("OPEN_RISK");
    }

    @Test
    void highOpenRisk_blocksWhenRequired() {
        var out = engine.evaluate(base(active(), List.of(), List.of(), null, false, List.of(), List.of(),
                false, null, false, true, 0, 3, null));
        assertThat(out.failedGates()).contains("OPEN_RISK");
    }

    @Test
    void openRisks_ignoredWhenGateNotRequired() {
        var out = engine.evaluate(base(active(), List.of(), List.of(), null, false, List.of(), List.of(),
                false, null, false, false, 5, 5, null));
        assertThat(out.failedGates()).doesNotContain("OPEN_RISK");
        assertThat(out.overallEligible()).isTrue();
    }

    @Test
    void blockedLifecycle_failsLifecycleGate() {
        var out = engine.evaluate(base(active(), List.of(), List.of(), null, false, List.of(), List.of(),
                false, null, false, false, 0, 0, "BLOCKED"));
        assertThat(out.failedGates()).contains("LIFECYCLE_STATUS");
        assertThat(out.overallEligible()).isFalse();
    }

    @Test
    void suspendedLifecycle_failsLifecycleGate() {
        var out = engine.evaluate(base(active(), List.of(), List.of(), null, false, List.of(), List.of(),
                false, null, false, false, 0, 0, "SUSPENDED"));
        assertThat(out.failedGates()).contains("LIFECYCLE_STATUS");
    }

    @Test
    void qualifiedLifecycle_passesLifecycleGate() {
        var out = engine.evaluate(base(active(), List.of(), List.of(), null, false, List.of(), List.of(),
                false, null, false, false, 0, 0, "QUALIFIED"));
        assertThat(out.failedGates()).doesNotContain("LIFECYCLE_STATUS");
        assertThat(out.overallEligible()).isTrue();
    }
}
