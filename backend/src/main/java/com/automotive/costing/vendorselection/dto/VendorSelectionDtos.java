package com.automotive.costing.vendorselection.dto;

import com.automotive.costing.vendorselection.entity.CapabilityStatus;
import com.automotive.costing.vendorselection.entity.CapabilityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class VendorSelectionDtos {
    private VendorSelectionDtos() {}

    public record CreateCapabilityRequest(
            @NotBlank String supplierId,
            @NotNull CapabilityType capabilityType,
            @NotBlank String capabilityCode,
            @NotBlank String capabilityName,
            String processCode,
            String commodityCode,
            String plantCode,
            BigDecimal maxAnnualCapacity,
            String capacityUom,
            boolean certified,
            LocalDate validFrom,
            LocalDate validTo,
            String notes
    ) {}

    public record CapabilityResponse(
            String capabilityId,
            String supplierId,
            CapabilityType capabilityType,
            String capabilityCode,
            String capabilityName,
            String processCode,
            String commodityCode,
            String plantCode,
            BigDecimal maxAnnualCapacity,
            String capacityUom,
            boolean certified,
            LocalDate validFrom,
            LocalDate validTo,
            CapabilityStatus status,
            String notes
    ) {}

    public record EvaluateEligibilityRequest(
            @NotBlank String supplierId,
            String bomNodeId,
            String programId,
            /** Required capability codes the part/process needs (mandatory gate). */
            List<String> requiredCapabilityCodes,
            /** Minimum annual capacity if capacity gate applies; null = skip. */
            BigDecimal requiredMinCapacity,
            /** When true, supplier must have at least one certified capability among required. */
            boolean requireCertification,
            /**
             * Required policy types (e.g. QUALITY_POLICY, ENVIRONMENTAL_POLICY).
             * Only VERIFIED, non-expired evidence satisfies the gate. Empty/null = gate not applied.
             */
            List<String> requiredPolicyTypes,
            /**
             * When true, latest questionnaire evaluation must be overallEligible.
             * Missing evaluation snapshot fails the QUESTIONNAIRE gate.
             */
            boolean requireQuestionnaire,
            /** When true, any CRITICAL open/mitigating risk fails OPEN_RISK gate. */
            boolean requireNoCriticalOpenRisks,
            /** When true, any HIGH open/mitigating risk fails OPEN_RISK gate. */
            boolean requireNoHighOpenRisks
    ) {}

    public record GateResult(
            String gateCode,
            boolean passed,
            String detail
    ) {}

    public record EligibilityResponse(
            String resultId,
            String supplierId,
            String bomNodeId,
            String programId,
            boolean overallEligible,
            List<String> failedGates,
            List<GateResult> gates,
            Instant evaluatedAt,
            String questionnaireEvaluationId,
            Boolean questionnaireEligible,
            int openCriticalRiskCount,
            int openHighRiskCount,
            int openRiskCount,
            String lifecycleStatus,
            boolean lifecycleBlocked
    ) {}


    public record CreateQualityMetricRequest(
            @NotBlank String supplierId,
            @NotNull com.automotive.costing.vendorselection.entity.QualityMetricType metricType,
            @NotNull java.time.LocalDate periodStart,
            @NotNull java.time.LocalDate periodEnd,
            @NotNull BigDecimal metricValue,
            String unit,
            Integer sampleSize,
            String evidenceRef,
            String notes
    ) {}

    public record QualityMetricResponse(
            String metricId, String supplierId,
            com.automotive.costing.vendorselection.entity.QualityMetricType metricType,
            java.time.LocalDate periodStart, java.time.LocalDate periodEnd,
            BigDecimal metricValue, String unit, Integer sampleSize,
            com.automotive.costing.vendorselection.entity.EvidenceStatus evidenceStatus,
            String evidenceRef, String verifiedBy, Instant verifiedAt, String notes
    ) {}

    public record CreateDeliveryMetricRequest(
            @NotBlank String supplierId,
            @NotNull java.time.LocalDate periodStart,
            @NotNull java.time.LocalDate periodEnd,
            BigDecimal otdPercent,
            BigDecimal otifPercent,
            Integer lateDeliveryCount,
            BigDecimal avgDelayDays,
            String evidenceRef,
            String notes
    ) {}

    public record DeliveryMetricResponse(
            String metricId, String supplierId,
            java.time.LocalDate periodStart, java.time.LocalDate periodEnd,
            BigDecimal otdPercent, BigDecimal otifPercent,
            Integer lateDeliveryCount, BigDecimal avgDelayDays,
            com.automotive.costing.vendorselection.entity.EvidenceStatus evidenceStatus,
            String evidenceRef, String verifiedBy, Instant verifiedAt, String notes
    ) {}

    public record CreateWarrantyRequest(
            @NotBlank String supplierId,
            Integer durationMonths,
            String coverageSummary,
            String exclusions,
            Integer claimSlaDays,
            String remedy,
            java.time.LocalDate effectiveFrom,
            java.time.LocalDate effectiveTo,
            BigDecimal claimRate,
            BigDecimal historicalCost,
            String currencyCode,
            String evidenceRef,
            String notes
    ) {}

    public record WarrantyResponse(
            String warrantyId, String supplierId, Integer durationMonths,
            String coverageSummary, String exclusions, Integer claimSlaDays, String remedy,
            java.time.LocalDate effectiveFrom, java.time.LocalDate effectiveTo,
            BigDecimal claimRate, BigDecimal historicalCost, String currencyCode,
            com.automotive.costing.vendorselection.entity.EvidenceStatus evidenceStatus,
            String evidenceRef, String verifiedBy, Instant verifiedAt, String notes
    ) {}

    public record CreateGuaranteeRequest(
            @NotBlank String supplierId,
            @NotNull com.automotive.costing.vendorselection.entity.GuaranteeType guaranteeType,
            String description,
            Integer periodMonths,
            String conditionsText,
            String failureConsequence,
            java.time.LocalDate effectiveFrom,
            java.time.LocalDate effectiveTo,
            String evidenceRef,
            String notes
    ) {}

    public record GuaranteeResponse(
            String guaranteeId, String supplierId,
            com.automotive.costing.vendorselection.entity.GuaranteeType guaranteeType,
            String description, Integer periodMonths, String conditionsText, String failureConsequence,
            java.time.LocalDate effectiveFrom, java.time.LocalDate effectiveTo,
            com.automotive.costing.vendorselection.entity.EvidenceStatus evidenceStatus,
            String evidenceRef, String verifiedBy, Instant verifiedAt, String notes
    ) {}

    public record VerifyEvidenceRequest(
            @NotNull com.automotive.costing.vendorselection.entity.EvidenceStatus evidenceStatus,
            String evidenceRef
    ) {}

    /** Policy / compliance evidence — declaration_only=true is never treated as verified. */
    public record CreatePolicyEvidenceRequest(
            @NotBlank String supplierId,
            @NotNull com.automotive.costing.vendorselection.entity.PolicyType policyType,
            @NotBlank String policyTitle,
            String description,
            String policyVersion,
            java.time.LocalDate effectiveFrom,
            java.time.LocalDate effectiveTo,
            java.time.LocalDate expiryDate,
            boolean declarationOnly,
            String evidenceRef,
            String notes
    ) {}

    public record PolicyEvidenceResponse(
            String policyId,
            String supplierId,
            com.automotive.costing.vendorselection.entity.PolicyType policyType,
            String policyTitle,
            String description,
            String policyVersion,
            java.time.LocalDate effectiveFrom,
            java.time.LocalDate effectiveTo,
            java.time.LocalDate expiryDate,
            boolean declarationOnly,
            com.automotive.costing.vendorselection.entity.EvidenceStatus evidenceStatus,
            String evidenceRef,
            String verifiedBy,
            Instant verifiedAt,
            String waiverReason,
            String notes
    ) {}

    public record VerifyPolicyRequest(
            @NotNull com.automotive.costing.vendorselection.entity.EvidenceStatus evidenceStatus,
            String evidenceRef,
            /** Required when evidenceStatus = WAIVED. */
            String waiverReason
    ) {}

    public record PolicySummaryResponse(
            String supplierId,
            long totalCount,
            long verifiedCount,
            long pendingCount,
            long failedOrExpiredCount,
            long waivedCount,
            long declarationOnlyCount,
            java.util.List<PolicyEvidenceResponse> items
    ) {}

    public record CreateWarrantyClaimRequest(
            @NotBlank String supplierId,
            String warrantyId,
            @NotNull java.time.LocalDate claimDate,
            String claimNumber,
            String partNumber,
            BigDecimal claimCost,
            String currencyCode,
            com.automotive.costing.vendorselection.entity.WarrantyClaimStatus claimStatus,
            Boolean acceptedFlag,
            Integer resolutionDays,
            String resolutionSummary,
            String evidenceRef,
            String notes
    ) {}

    public record WarrantyClaimResponse(
            String claimId, String supplierId, String warrantyId, java.time.LocalDate claimDate,
            String claimNumber, String partNumber, BigDecimal claimCost, String currencyCode,
            com.automotive.costing.vendorselection.entity.WarrantyClaimStatus claimStatus,
            Boolean acceptedFlag, Integer resolutionDays, String resolutionSummary,
            com.automotive.costing.vendorselection.entity.EvidenceStatus evidenceStatus,
            String evidenceRef, String verifiedBy, Instant verifiedAt, String notes
    ) {}

    public record WarrantyClaimSummaryResponse(
            String supplierId, long totalClaims, long openClaims, long acceptedClaims, long rejectedClaims,
            long closedClaims, long withdrawnClaims,
            BigDecimal totalClaimCost, String currencyCode, java.util.List<WarrantyClaimResponse> claims
    ) {}

    public record UpdateWarrantyClaimStatusRequest(
            @NotNull com.automotive.costing.vendorselection.entity.WarrantyClaimStatus claimStatus,
            Boolean acceptedFlag,
            Integer resolutionDays,
            String resolutionSummary,
            String notes
    ) {}

    public record CreateGuaranteeEventRequest(
            @NotBlank String supplierId, String guaranteeId, @NotNull java.time.LocalDate eventDate,
            @NotNull com.automotive.costing.vendorselection.entity.GuaranteeEventType eventType,
            String description, String consequenceApplied, BigDecimal penaltyAmount, String currencyCode,
            boolean resolvedFlag, String resolutionSummary, String evidenceRef, String notes
    ) {}

    public record GuaranteeEventResponse(
            String eventId, String supplierId, String guaranteeId, java.time.LocalDate eventDate,
            com.automotive.costing.vendorselection.entity.GuaranteeEventType eventType,
            String description, String consequenceApplied, BigDecimal penaltyAmount, String currencyCode,
            boolean resolvedFlag, String resolutionSummary,
            com.automotive.costing.vendorselection.entity.EvidenceStatus evidenceStatus,
            String evidenceRef, String verifiedBy, Instant verifiedAt, String notes
    ) {}

    public record CreateAfterSalesMetricRequest(
            @NotBlank String supplierId, @NotNull java.time.LocalDate periodStart, @NotNull java.time.LocalDate periodEnd,
            BigDecimal responseTimeHours, BigDecimal resolutionTimeHours, Boolean emergencySupportFlag,
            Boolean fieldSupportFlag, Boolean sparePartsAvailable, Boolean localSupportFlag,
            Integer unresolvedComplaints, BigDecimal avgClosureDays, BigDecimal serviceSlaMetPercent,
            String evidenceRef, String notes
    ) {}

    public record AfterSalesMetricResponse(
            String metricId, String supplierId, java.time.LocalDate periodStart, java.time.LocalDate periodEnd,
            BigDecimal responseTimeHours, BigDecimal resolutionTimeHours, Boolean emergencySupportFlag,
            Boolean fieldSupportFlag, Boolean sparePartsAvailable, Boolean localSupportFlag,
            Integer unresolvedComplaints, BigDecimal avgClosureDays, BigDecimal serviceSlaMetPercent,
            com.automotive.costing.vendorselection.entity.EvidenceStatus evidenceStatus,
            String evidenceRef, String verifiedBy, Instant verifiedAt, String notes
    ) {}

    public record PerformanceFeedbackResponse(
            String feedbackId, String supplierId, java.time.LocalDate feedbackDate,
            com.automotive.costing.vendorselection.entity.PerformanceFeedbackType feedbackType,
            String metricCode, BigDecimal metricValue, String unit,
            com.automotive.costing.vendorselection.entity.EvidenceStatus evidenceStatus,
            boolean appliedToHistory, String historyMetricId, String historyMetricKind, String narrative
    ) {}


    public record AfterSalesSummaryResponse(
            String supplierId,
            long totalMetrics,
            long verifiedMetrics,
            long pendingMetrics,
            BigDecimal latestSlaMetPercent,
            Integer latestUnresolvedComplaints,
            java.util.List<AfterSalesMetricResponse> metrics
    ) {}

    public record GuaranteeEventSummaryResponse(
            String supplierId,
            long totalEvents,
            long unresolvedEvents,
            long verifiedEvents,
            BigDecimal totalPenaltyAmount,
            String currencyCode,
            java.util.List<GuaranteeEventResponse> events
    ) {}

    public record EvidencePendingSummaryResponse(
            String supplierId,
            long pendingPolicy,
            long pendingQuality,
            long pendingDelivery,
            long pendingWarrantyClaims,
            long pendingGuaranteeEvents,
            long pendingAfterSales,
            long pendingPerformanceFeedback,
            long totalPending
    ) {}

    public record Supplier360Response(
            String supplierId,
            java.util.List<QualityMetricResponse> qualityMetrics,
            java.util.List<DeliveryMetricResponse> deliveryMetrics,
            java.util.List<WarrantyResponse> warranties,
            java.util.List<WarrantyClaimResponse> warrantyClaims,
            java.util.List<GuaranteeResponse> guarantees,
            java.util.List<GuaranteeEventResponse> guaranteeEvents,
            java.util.List<AfterSalesMetricResponse> afterSalesMetrics,
            java.util.List<PerformanceFeedbackResponse> performanceFeedback,
            java.util.List<CapabilityResponse> capabilities,
            java.util.List<PolicyEvidenceResponse> policies
    ) {}

}
