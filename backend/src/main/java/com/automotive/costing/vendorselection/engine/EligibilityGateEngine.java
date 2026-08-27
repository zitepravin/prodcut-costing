package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.rfq.entity.SupplierMaster;
import com.automotive.costing.rfq.entity.SupplierStatus;
import com.automotive.costing.vendorselection.dto.VendorSelectionDtos.GateResult;
import com.automotive.costing.vendorselection.entity.CapabilityStatus;
import com.automotive.costing.vendorselection.entity.SupplierCapability;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deterministic mandatory eligibility gates.
 * Pure logic — no I/O. Policy and questionnaire facts are pre-resolved by the service layer.
 */
@Component
public class EligibilityGateEngine {

    /**
     * @param questionnaireEligible latest questionnaire overallEligible when requireQuestionnaire; null if none
     * @param requireQuestionnaire  when true, QUESTIONNAIRE gate is applied
     */
    public record EvaluationInput(
            SupplierMaster supplier,
            List<SupplierCapability> activeCapabilities,
            List<String> requiredCapabilityCodes,
            BigDecimal requiredMinCapacity,
            boolean requireCertification,
            List<String> verifiedNonExpiredPolicyTypes,
            List<String> requiredPolicyTypes,
            boolean requireQuestionnaire,
            Boolean questionnaireEligible,
            boolean requireNoCriticalOpenRisks,
            boolean requireNoHighOpenRisks,
            int openCriticalRiskCount,
            int openHighRiskCount,
            String lifecycleStatus,
            LocalDate asOf
    ) {}

    public record EvaluationOutput(
            boolean overallEligible,
            List<GateResult> gates,
            List<String> failedGates
    ) {}

    public EvaluationOutput evaluate(EvaluationInput input) {
        List<GateResult> gates = new ArrayList<>();
        LocalDate asOf = input.asOf() != null ? input.asOf() : LocalDate.now();

        boolean statusOk = input.supplier() != null && input.supplier().getStatus() == SupplierStatus.ACTIVE;
        gates.add(new GateResult("SUPPLIER_STATUS", statusOk,
                statusOk ? "Supplier is ACTIVE" : "Supplier is not ACTIVE"));

        List<SupplierCapability> validCaps = input.activeCapabilities() == null ? List.of()
                : input.activeCapabilities().stream()
                .filter(c -> c.getStatus() == CapabilityStatus.ACTIVE)
                .filter(c -> c.getValidFrom() == null || !asOf.isBefore(c.getValidFrom()))
                .filter(c -> c.getValidTo() == null || !asOf.isAfter(c.getValidTo()))
                .toList();

        List<String> required = input.requiredCapabilityCodes() == null ? List.of()
                : input.requiredCapabilityCodes().stream().filter(s -> s != null && !s.isBlank()).toList();
        Set<String> have = new HashSet<>();
        for (SupplierCapability c : validCaps) {
            have.add(c.getCapabilityCode());
        }
        boolean capOk = true;
        String capDetail;
        if (required.isEmpty()) {
            capDetail = "No required capability codes specified";
        } else {
            List<String> missing = required.stream().filter(r -> !have.contains(r)).toList();
            capOk = missing.isEmpty();
            capDetail = capOk ? "All required capabilities present" : "Missing: " + String.join(", ", missing);
        }
        gates.add(new GateResult("PART_PROCESS_CAPABILITY", capOk, capDetail));

        boolean capacityOk = true;
        String capacityDetail = "Capacity gate not applied";
        if (input.requiredMinCapacity() != null) {
            BigDecimal maxCap = validCaps.stream()
                    .map(SupplierCapability::getMaxAnnualCapacity)
                    .filter(v -> v != null)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            capacityOk = maxCap.compareTo(input.requiredMinCapacity()) >= 0;
            capacityDetail = capacityOk
                    ? "Capacity " + maxCap + " meets minimum " + input.requiredMinCapacity()
                    : "Capacity " + maxCap + " below minimum " + input.requiredMinCapacity();
        }
        gates.add(new GateResult("CAPACITY", capacityOk, capacityDetail));

        boolean certOk = true;
        String certDetail = "Certification gate not applied";
        if (input.requireCertification()) {
            if (required.isEmpty()) {
                certOk = validCaps.stream().anyMatch(SupplierCapability::isCertified);
                certDetail = certOk ? "At least one certified capability" : "No certified capability";
            } else {
                certOk = validCaps.stream()
                        .filter(c -> required.contains(c.getCapabilityCode()))
                        .anyMatch(SupplierCapability::isCertified);
                certDetail = certOk ? "Required capability is certified" : "Required capability not certified";
            }
        }
        gates.add(new GateResult("CERTIFICATION", certOk, certDetail));

        // POLICY_COMPLIANCE
        List<String> requiredPolicies = input.requiredPolicyTypes() == null ? List.of()
                : input.requiredPolicyTypes().stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .map(String::toUpperCase)
                .toList();
        Set<String> verifiedPolicies = input.verifiedNonExpiredPolicyTypes() == null ? Set.of()
                : input.verifiedNonExpiredPolicyTypes().stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        boolean policyOk = true;
        String policyDetail;
        if (requiredPolicies.isEmpty()) {
            policyDetail = "Policy compliance gate not applied";
        } else {
            List<String> missingPolicies = requiredPolicies.stream()
                    .filter(p -> !verifiedPolicies.contains(p))
                    .toList();
            policyOk = missingPolicies.isEmpty();
            policyDetail = policyOk
                    ? "All required policy types have VERIFIED non-expired evidence"
                    : "Missing verified non-expired policy evidence: " + String.join(", ", missingPolicies);
        }
        gates.add(new GateResult("POLICY_COMPLIANCE", policyOk, policyDetail));

        // V46: QUESTIONNAIRE — latest evaluation overallEligible when required
        boolean questionnaireOk = true;
        String questionnaireDetail;
        if (!input.requireQuestionnaire()) {
            questionnaireDetail = "Questionnaire gate not applied";
        } else if (input.questionnaireEligible() == null) {
            questionnaireOk = false;
            questionnaireDetail = "No questionnaire evaluation snapshot found — run questionnaire evaluate first";
        } else if (Boolean.TRUE.equals(input.questionnaireEligible())) {
            questionnaireDetail = "Latest questionnaire evaluation is eligible";
        } else {
            questionnaireOk = false;
            questionnaireDetail = "Latest questionnaire evaluation is NOT eligible (mandatory gates or required answers failed)";
        }
        gates.add(new GateResult("QUESTIONNAIRE", questionnaireOk, questionnaireDetail));

        // V47: OPEN_RISK — critical/high open risks must not disappear inside score
        boolean riskOk = true;
        String riskDetail;
        if (!input.requireNoCriticalOpenRisks() && !input.requireNoHighOpenRisks()) {
            riskDetail = "Open risk gate not applied";
        } else {
            List<String> riskFails = new ArrayList<>();
            if (input.requireNoCriticalOpenRisks() && input.openCriticalRiskCount() > 0) {
                riskFails.add("critical open risks=" + input.openCriticalRiskCount());
            }
            if (input.requireNoHighOpenRisks() && input.openHighRiskCount() > 0) {
                riskFails.add("high open risks=" + input.openHighRiskCount());
            }
            riskOk = riskFails.isEmpty();
            riskDetail = riskOk
                    ? "No blocking open risks (critical=" + input.openCriticalRiskCount()
                      + ", high=" + input.openHighRiskCount() + ")"
                    : "Blocking open risks: " + String.join("; ", riskFails);
        }
        gates.add(new GateResult("OPEN_RISK", riskOk, riskDetail));

        // V59: LIFECYCLE_STATUS — BLOCKED/SUSPENDED cannot pass eligibility
        boolean lifecycleOk = true;
        String lifecycleDetail;
        String lc = input.lifecycleStatus();
        if (lc == null || lc.isBlank()) {
            lifecycleDetail = "No qualification lifecycle recorded — gate not blocked";
        } else {
            String upper = lc.trim().toUpperCase();
            if ("BLOCKED".equals(upper) || "SUSPENDED".equals(upper)) {
                lifecycleOk = false;
                lifecycleDetail = "Supplier lifecycle is " + upper + " — not eligible until reinstated";
            } else {
                lifecycleDetail = "Lifecycle status " + upper + " does not block eligibility";
            }
        }
        gates.add(new GateResult("LIFECYCLE_STATUS", lifecycleOk, lifecycleDetail));

        List<String> failed = gates.stream().filter(g -> !g.passed()).map(GateResult::gateCode).toList();
        return new EvaluationOutput(failed.isEmpty(), gates, failed);
    }
}
