package com.automotive.costing.vendorselection.controller;

import com.automotive.costing.vendorselection.dto.VendorSelectionDtos.CapabilityResponse;
import com.automotive.costing.vendorselection.dto.VendorSelectionDtos.CreateCapabilityRequest;
import com.automotive.costing.vendorselection.dto.VendorSelectionDtos.EligibilityResponse;
import com.automotive.costing.vendorselection.dto.VendorSelectionDtos.EvaluateEligibilityRequest;
import com.automotive.costing.platform.access.RequiresModule;
import com.automotive.costing.vendorselection.service.VendorCapabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vendor-selection")
@RequiredArgsConstructor
@RequiresModule("VENDOR_SELECTION")
@Tag(name = "Vendor Selection (B1)")
public class VendorSelectionController {

    private final VendorCapabilityService vendorCapabilityService;
    private final com.automotive.costing.vendorselection.service.SupplierEvidenceService supplierEvidenceService;
    private final com.automotive.costing.vendorselection.service.SupplierPerformanceFeedbackService performanceFeedbackService;
    private final com.automotive.costing.vendorselection.service.TcoCalculationService tcoCalculationService;
    private final com.automotive.costing.vendorselection.service.ScorecardService scorecardService;
    private final com.automotive.costing.vendorselection.service.VendorRecommendationService vendorRecommendationService;
    private final com.automotive.costing.vendorselection.service.QuestionnaireService questionnaireService;
    private final com.automotive.costing.vendorselection.service.SupplierComparisonService supplierComparisonService;
    private final com.automotive.costing.vendorselection.service.SupplierQualificationService supplierQualificationService;
    private final com.automotive.costing.vendorselection.service.SupplierLifecycleGuard supplierLifecycleGuard;
    private final com.automotive.costing.vendorselection.service.CapabilityExpiryService capabilityExpiryService;
    private final com.automotive.costing.vendorselection.service.SupplierRiskService supplierRiskService;
    private final com.automotive.costing.vendorselection.service.EvidenceExpiryService evidenceExpiryService;
    private final com.automotive.costing.vendorselection.service.RecommendationDecisionWebhookService decisionWebhookService;

    @PostMapping("/capabilities")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Register supplier part/process capability")
    public ResponseEntity<CapabilityResponse> create(@Valid @RequestBody CreateCapabilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vendorCapabilityService.create(request));
    }

    @GetMapping("/suppliers/{supplierId}/capabilities")
    @Operation(summary = "List capabilities (optional type filter; activeOnly default true)")
    public ResponseEntity<?> listCapabilities(
            @PathVariable String supplierId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "true") boolean activeOnly) {
        com.automotive.costing.vendorselection.entity.CapabilityType capType = null;
        if (type != null && !type.isBlank()) {
            try {
                capType = com.automotive.costing.vendorselection.entity.CapabilityType.valueOf(type.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new com.automotive.costing.common.exception.BusinessRuleViolationException(
                        "Unknown capability type: " + type, "VS-CAP-10");
            }
        }
        return ResponseEntity.ok(vendorCapabilityService.listBySupplier(supplierId, capType, activeOnly));
    }

    @GetMapping("/capabilities/expiring")
    @Operation(summary = "ACTIVE capabilities with valid_to on or before asOfDate (default today)")
    public ResponseEntity<?> listExpiringCapabilities(
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate asOfDate) {
        return ResponseEntity.ok(vendorCapabilityService.listExpiring(asOfDate));
    }

    @PostMapping("/eligibility/evaluate")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'COSTING_ENGINEER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Run mandatory eligibility gates for a supplier (B1)")
    public ResponseEntity<EligibilityResponse> evaluate(@Valid @RequestBody EvaluateEligibilityRequest request) {
        return ResponseEntity.ok(vendorCapabilityService.evaluate(request));
    }

    @PostMapping("/quality-metrics")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Record supplier quality metric (evidence starts PENDING)")
    public ResponseEntity<com.automotive.costing.vendorselection.dto.VendorSelectionDtos.QualityMetricResponse> addQuality(
            @Valid @RequestBody com.automotive.costing.vendorselection.dto.VendorSelectionDtos.CreateQualityMetricRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierEvidenceService.addQuality(request));
    }

    @PostMapping("/quality-metrics/{metricId}/verify")
    @PreAuthorize("hasAnyRole('SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Verify or fail quality metric evidence")
    public ResponseEntity<?> verifyQuality(@PathVariable String metricId,
            @Valid @RequestBody com.automotive.costing.vendorselection.dto.VendorSelectionDtos.VerifyEvidenceRequest request) {
        return ResponseEntity.ok(supplierEvidenceService.verifyQuality(metricId, request));
    }

    @GetMapping("/suppliers/{supplierId}/quality-metrics")
    @Operation(summary = "List quality metrics (optional evidenceStatus filter)")
    public ResponseEntity<?> listQuality(
            @PathVariable String supplierId,
            @RequestParam(required = false) String evidenceStatus) {
        com.automotive.costing.vendorselection.entity.EvidenceStatus es = null;
        if (evidenceStatus != null && !evidenceStatus.isBlank()) {
            try {
                es = com.automotive.costing.vendorselection.entity.EvidenceStatus.valueOf(evidenceStatus.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new com.automotive.costing.common.exception.BusinessRuleViolationException(
                        "Unknown evidence status: " + evidenceStatus, "VS-QM-10");
            }
        }
        return ResponseEntity.ok(supplierEvidenceService.listQuality(supplierId, es));
    }

    @PostMapping("/delivery-metrics")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Record supplier delivery metric")
    public ResponseEntity<?> addDelivery(
            @Valid @RequestBody com.automotive.costing.vendorselection.dto.VendorSelectionDtos.CreateDeliveryMetricRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierEvidenceService.addDelivery(request));
    }

    @PostMapping("/delivery-metrics/{metricId}/verify")
    @PreAuthorize("hasAnyRole('SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<?> verifyDelivery(@PathVariable String metricId,
            @Valid @RequestBody com.automotive.costing.vendorselection.dto.VendorSelectionDtos.VerifyEvidenceRequest request) {
        return ResponseEntity.ok(supplierEvidenceService.verifyDelivery(metricId, request));
    }

    @GetMapping("/suppliers/{supplierId}/delivery-metrics")
    @Operation(summary = "List delivery metrics (optional evidenceStatus filter)")
    public ResponseEntity<?> listDelivery(
            @PathVariable String supplierId,
            @RequestParam(required = false) String evidenceStatus) {
        com.automotive.costing.vendorselection.entity.EvidenceStatus es = null;
        if (evidenceStatus != null && !evidenceStatus.isBlank()) {
            try {
                es = com.automotive.costing.vendorselection.entity.EvidenceStatus.valueOf(evidenceStatus.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new com.automotive.costing.common.exception.BusinessRuleViolationException(
                        "Unknown evidence status: " + evidenceStatus, "VS-DM-10");
            }
        }
        return ResponseEntity.ok(supplierEvidenceService.listDelivery(supplierId, es));
    }

    @PostMapping("/warranties")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<?> addWarranty(
            @Valid @RequestBody com.automotive.costing.vendorselection.dto.VendorSelectionDtos.CreateWarrantyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierEvidenceService.addWarranty(request));
    }

    @PostMapping("/warranties/{warrantyId}/verify")
    @PreAuthorize("hasAnyRole('SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<?> verifyWarranty(@PathVariable String warrantyId,
            @Valid @RequestBody com.automotive.costing.vendorselection.dto.VendorSelectionDtos.VerifyEvidenceRequest request) {
        return ResponseEntity.ok(supplierEvidenceService.verifyWarranty(warrantyId, request));
    }

    @PostMapping("/warranty-claims")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Record a warranty claim (evidence starts PENDING)")
    public ResponseEntity<?> addWarrantyClaim(
            @Valid @RequestBody com.automotive.costing.vendorselection.dto.VendorSelectionDtos.CreateWarrantyClaimRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierEvidenceService.addWarrantyClaim(request));
    }

    @PostMapping("/warranty-claims/{claimId}/verify")
    @PreAuthorize("hasAnyRole('SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<?> verifyWarrantyClaim(@PathVariable String claimId,
            @Valid @RequestBody com.automotive.costing.vendorselection.dto.VendorSelectionDtos.VerifyEvidenceRequest request) {
        return ResponseEntity.ok(supplierEvidenceService.verifyWarrantyClaim(claimId, request));
    }

    @PostMapping("/warranty-claims/{claimId}/status")
    @Operation(summary = "Transition warranty claim commercial status (guarded graph; independent of evidence)")
    public ResponseEntity<?> updateWarrantyClaimStatus(@PathVariable String claimId,
            @Valid @RequestBody com.automotive.costing.vendorselection.dto.VendorSelectionDtos.UpdateWarrantyClaimStatusRequest request) {
        return ResponseEntity.ok(supplierEvidenceService.updateWarrantyClaimStatus(claimId, request));
    }

    @GetMapping("/suppliers/{supplierId}/warranty-claims")
    @Operation(summary = "Warranty claim summary for supplier")
    public ResponseEntity<?> warrantyClaimSummary(@PathVariable String supplierId) {
        return ResponseEntity.ok(supplierEvidenceService.warrantyClaimSummary(supplierId));
    }

    @GetMapping("/suppliers/{supplierId}/warranty-claims/items")
    @Operation(summary = "List warranty claims (optional claimStatus or evidenceStatus)")
    public ResponseEntity<?> listWarrantyClaims(
            @PathVariable String supplierId,
            @RequestParam(required = false) String claimStatus,
            @RequestParam(required = false) String evidenceStatus) {
        com.automotive.costing.vendorselection.entity.WarrantyClaimStatus cs = null;
        com.automotive.costing.vendorselection.entity.EvidenceStatus es = null;
        try {
            if (claimStatus != null && !claimStatus.isBlank()) {
                cs = com.automotive.costing.vendorselection.entity.WarrantyClaimStatus.valueOf(claimStatus.trim().toUpperCase());
            }
            if (evidenceStatus != null && !evidenceStatus.isBlank()) {
                es = com.automotive.costing.vendorselection.entity.EvidenceStatus.valueOf(evidenceStatus.trim().toUpperCase());
            }
        } catch (IllegalArgumentException ex) {
            throw new com.automotive.costing.common.exception.BusinessRuleViolationException(
                    "Invalid claimStatus or evidenceStatus", "VS-WC-10");
        }
        return ResponseEntity.ok(supplierEvidenceService.listWarrantyClaims(supplierId, cs, es));
    }

    @GetMapping("/suppliers/{supplierId}/guarantee-events/items")
    @Operation(summary = "List guarantee events (optional unresolvedOnly / evidenceStatus)")
    public ResponseEntity<?> listGuaranteeEvents(
            @PathVariable String supplierId,
            @RequestParam(required = false) Boolean unresolvedOnly,
            @RequestParam(required = false) String evidenceStatus) {
        com.automotive.costing.vendorselection.entity.EvidenceStatus es = null;
        if (evidenceStatus != null && !evidenceStatus.isBlank()) {
            try {
                es = com.automotive.costing.vendorselection.entity.EvidenceStatus.valueOf(evidenceStatus.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new com.automotive.costing.common.exception.BusinessRuleViolationException(
                        "Unknown evidence status: " + evidenceStatus, "VS-GE-10");
            }
        }
        return ResponseEntity.ok(supplierEvidenceService.listGuaranteeEvents(supplierId, unresolvedOnly, es));
    }

    @GetMapping("/suppliers/{supplierId}/evidence-pending-summary")
    @Operation(summary = "Pending evidence counts across policy/quality/delivery/claims/events/after-sales/feedback")
    public ResponseEntity<?> evidencePendingSummary(@PathVariable String supplierId) {
        return ResponseEntity.ok(supplierEvidenceService.evidencePendingSummary(supplierId));
    }

    @GetMapping("/suppliers/{supplierId}/after-sales-metrics")
    @Operation(summary = "List after-sales metrics (optional evidenceStatus filter)")
    public ResponseEntity<?> listAfterSales(
            @PathVariable String supplierId,
            @RequestParam(required = false) String evidenceStatus) {
        com.automotive.costing.vendorselection.entity.EvidenceStatus es = null;
        if (evidenceStatus != null && !evidenceStatus.isBlank()) {
            try {
                es = com.automotive.costing.vendorselection.entity.EvidenceStatus.valueOf(evidenceStatus.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new com.automotive.costing.common.exception.BusinessRuleViolationException(
                        "Unknown evidence status: " + evidenceStatus, "VS-AS-10");
            }
        }
        return ResponseEntity.ok(supplierEvidenceService.listAfterSales(supplierId, es));
    }

    @GetMapping("/suppliers/{supplierId}/after-sales-summary")
    @Operation(summary = "After-sales metrics summary for supplier")
    public ResponseEntity<?> afterSalesSummary(@PathVariable String supplierId) {
        return ResponseEntity.ok(supplierEvidenceService.afterSalesSummary(supplierId));
    }

    @GetMapping("/suppliers/{supplierId}/guarantee-events-summary")
    @Operation(summary = "Guarantee event summary for supplier")
    public ResponseEntity<?> guaranteeEventSummary(@PathVariable String supplierId) {
        return ResponseEntity.ok(supplierEvidenceService.guaranteeEventSummary(supplierId));
    }

    @PostMapping("/guarantee-events")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Record a guarantee failure or related event")
    public ResponseEntity<?> addGuaranteeEvent(
            @Valid @RequestBody com.automotive.costing.vendorselection.dto.VendorSelectionDtos.CreateGuaranteeEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierEvidenceService.addGuaranteeEvent(request));
    }

    @PostMapping("/guarantee-events/{eventId}/verify")
    @PreAuthorize("hasAnyRole('SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<?> verifyGuaranteeEvent(@PathVariable String eventId,
            @Valid @RequestBody com.automotive.costing.vendorselection.dto.VendorSelectionDtos.VerifyEvidenceRequest request) {
        return ResponseEntity.ok(supplierEvidenceService.verifyGuaranteeEvent(eventId, request));
    }

    @PostMapping("/after-sales-metrics")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Record after-sales service metric (separate from warranty)")
    public ResponseEntity<?> addAfterSales(
            @Valid @RequestBody com.automotive.costing.vendorselection.dto.VendorSelectionDtos.CreateAfterSalesMetricRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierEvidenceService.addAfterSales(request));
    }

    @PostMapping("/after-sales-metrics/{metricId}/verify")
    @PreAuthorize("hasAnyRole('SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<?> verifyAfterSales(@PathVariable String metricId,
            @Valid @RequestBody com.automotive.costing.vendorselection.dto.VendorSelectionDtos.VerifyEvidenceRequest request) {
        return ResponseEntity.ok(supplierEvidenceService.verifyAfterSales(metricId, request));
    }

    @PostMapping("/performance-feedback")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Record actual supplier performance feedback (C3 history intake)")
    public ResponseEntity<?> createPerformanceFeedback(
            @Valid @RequestBody com.automotive.costing.vendorselection.service.SupplierPerformanceFeedbackService.CreateFeedbackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(performanceFeedbackService.create(request));
    }

    @PostMapping("/performance-feedback/{feedbackId}/verify")
    @PreAuthorize("hasAnyRole('SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<?> verifyPerformanceFeedback(@PathVariable String feedbackId,
            @Valid @RequestBody com.automotive.costing.vendorselection.service.SupplierPerformanceFeedbackService.VerifyFeedbackRequest request) {
        return ResponseEntity.ok(performanceFeedbackService.verify(feedbackId, request));
    }

    @PostMapping("/performance-feedback/{feedbackId}/apply-to-history")
    @PreAuthorize("hasAnyRole('SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Mark verified feedback as applied to supplier history (no auto score change)")
    public ResponseEntity<?> applyPerformanceFeedback(@PathVariable String feedbackId) {
        return ResponseEntity.ok(performanceFeedbackService.applyToHistory(feedbackId));
    }

    @GetMapping("/suppliers/{supplierId}/performance-feedback")
    @Operation(summary = "List performance feedback (optional appliedToHistory / evidenceStatus)")
    public ResponseEntity<?> listPerformanceFeedback(
            @PathVariable String supplierId,
            @RequestParam(required = false) Boolean appliedToHistory,
            @RequestParam(required = false) String evidenceStatus) {
        com.automotive.costing.vendorselection.entity.EvidenceStatus es = null;
        if (evidenceStatus != null && !evidenceStatus.isBlank()) {
            try {
                es = com.automotive.costing.vendorselection.entity.EvidenceStatus.valueOf(evidenceStatus.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new com.automotive.costing.common.exception.BusinessRuleViolationException(
                        "Unknown evidence status: " + evidenceStatus, "VS-FB-10");
            }
        }
        return ResponseEntity.ok(performanceFeedbackService.listBySupplier(supplierId, appliedToHistory, es));
    }

    @PostMapping("/guarantees")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<?> addGuarantee(
            @Valid @RequestBody com.automotive.costing.vendorselection.dto.VendorSelectionDtos.CreateGuaranteeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierEvidenceService.addGuarantee(request));
    }

    @PostMapping("/guarantees/{guaranteeId}/verify")
    @PreAuthorize("hasAnyRole('SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<?> verifyGuarantee(@PathVariable String guaranteeId,
            @Valid @RequestBody com.automotive.costing.vendorselection.dto.VendorSelectionDtos.VerifyEvidenceRequest request) {
        return ResponseEntity.ok(supplierEvidenceService.verifyGuarantee(guaranteeId, request));
    }

    @GetMapping("/suppliers/{supplierId}/360")
    @Operation(summary = "Supplier 360 view: quality, delivery, warranty, guarantee, capabilities")
    public ResponseEntity<?> supplier360(@PathVariable String supplierId) {
        return ResponseEntity.ok(supplierEvidenceService.supplier360(supplierId));
    }

    @PostMapping("/tco/calculate")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'COSTING_ENGINEER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Calculate supplier TCO (missing components marked DATA_NOT_AVAILABLE, never assumed zero)")
    public ResponseEntity<?> calculateTco(
            @RequestBody com.automotive.costing.vendorselection.service.TcoCalculationService.CalculateTcoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tcoCalculationService.calculate(request));
    }

    @GetMapping("/suppliers/{supplierId}/tco")
    @Operation(summary = "List TCO calculation history for a supplier")
    public ResponseEntity<?> listTco(@PathVariable String supplierId) {
        return ResponseEntity.ok(tcoCalculationService.listBySupplier(supplierId));
    }

    @PostMapping("/scorecards/templates")
    @PreAuthorize("hasAnyRole('SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Create scorecard template (weights must total 100%)")
    public ResponseEntity<?> createScorecardTemplate(
            @RequestBody com.automotive.costing.vendorselection.service.ScorecardService.CreateTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scorecardService.createTemplate(request));
    }

    @GetMapping("/scorecards/templates")
    @Operation(summary = "List scorecard templates")
    public ResponseEntity<?> listScorecardTemplates() {
        return ResponseEntity.ok(scorecardService.listTemplates());
    }

    @GetMapping("/scorecards/templates/{templateId}")
    public ResponseEntity<?> getScorecardTemplate(@PathVariable String templateId) {
        return ResponseEntity.ok(scorecardService.getTemplate(templateId));
    }

    @GetMapping("/scorecards/templates/{templateId}/lineage")
    @Operation(summary = "List scorecard template lineage versions (root → clones)")
    public ResponseEntity<?> listScorecardLineage(@PathVariable String templateId) {
        return ResponseEntity.ok(scorecardService.listLineage(templateId));
    }

    @PostMapping("/scorecards/templates/{templateId}/clone")
    @PreAuthorize("hasAnyRole('SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Clone scorecard template with optional customer-specific weights (must total 100%)")
    public ResponseEntity<?> cloneScorecardTemplate(@PathVariable String templateId,
            @RequestBody com.automotive.costing.vendorselection.service.ScorecardService.CloneTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scorecardService.cloneTemplate(templateId, request));
    }

    @PostMapping("/scorecards/templates/{templateId}/deactivate")
    @PreAuthorize("hasAnyRole('SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Retire template (P3-07a: cannot retire last ACTIVE/published set)")
    public ResponseEntity<?> deactivateScorecardTemplate(@PathVariable String templateId) {
        return ResponseEntity.ok(scorecardService.deactivateTemplate(templateId));
    }

    @PostMapping("/scorecards/templates/{templateId}/publish")
    @PreAuthorize("hasAnyRole('SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "P3-07a: publish DRAFT → ACTIVE (weights must total 100%)")
    public ResponseEntity<?> publishScorecardTemplate(@PathVariable String templateId) {
        return ResponseEntity.ok(scorecardService.publishTemplate(templateId));
    }

    @GetMapping("/scorecards/templates/effective")
    @Operation(summary = "P3-07a: resolve effective template (system default when id omitted)")
    public ResponseEntity<?> effectiveScorecardTemplate(
            @RequestParam(required = false) String templateId) {
        return ResponseEntity.ok(scorecardService.resolveEffectiveTemplate(templateId));
    }

    @PutMapping("/scorecards/templates/{templateId}/categories")
    @PreAuthorize("hasAnyRole('SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Replace categories/weights only if template never used; otherwise clone")
    public ResponseEntity<?> replaceScorecardCategories(@PathVariable String templateId,
            @RequestBody java.util.List<com.automotive.costing.vendorselection.service.ScorecardService.CategoryDef> categories) {
        return ResponseEntity.ok(scorecardService.replaceCategoriesIfUnused(templateId, categories));
    }

    @PostMapping("/scorecards/evaluate")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'COSTING_ENGINEER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Evaluate supplier against scorecard template")
    public ResponseEntity<?> evaluateScorecard(
            @RequestBody com.automotive.costing.vendorselection.service.ScorecardService.EvaluateScorecardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scorecardService.evaluate(request));
    }

    @GetMapping("/suppliers/{supplierId}/scorecard-results")
    @Operation(summary = "Scorecard evaluation history for supplier")
    public ResponseEntity<?> listScorecardResults(@PathVariable String supplierId) {
        return ResponseEntity.ok(scorecardService.listResultsBySupplier(supplierId));
    }

    @PostMapping("/recommendations")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'COSTING_ENGINEER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Create DRAFT recommendation ranking (not an approval)")
    public ResponseEntity<?> createRecommendation(
            @RequestBody com.automotive.costing.vendorselection.service.VendorRecommendationService.CreateRecommendationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vendorRecommendationService.createDraft(request));
    }

    @GetMapping("/recommendations")
    @Operation(summary = "List recommendations by status (e.g. SUBMITTED queue for human decision)")
    public ResponseEntity<?> listRecommendations(
            @org.springframework.web.bind.annotation.RequestParam String status) {
        try {
            var st = com.automotive.costing.vendorselection.entity.RecommendationStatus.valueOf(status.trim().toUpperCase());
            return ResponseEntity.ok(vendorRecommendationService.listByStatus(st));
        } catch (IllegalArgumentException ex) {
            throw new com.automotive.costing.common.exception.BusinessRuleViolationException(
                    "Unknown recommendation status: " + status, "VS-REC-11");
        }
    }

    @GetMapping("/recommendations/{id}")
    public ResponseEntity<?> getRecommendation(@PathVariable String id) {
        return ResponseEntity.ok(vendorRecommendationService.get(id));
    }

    @GetMapping("/recommendations/{id}/explanation")
    @Operation(summary = "Structured explainability snapshot (FACT / CALCULATED / INFERENCE / MISSING DATA)")
    public ResponseEntity<?> getRecommendationExplanation(@PathVariable String id) {
        return ResponseEntity.ok(vendorRecommendationService.getExplanation(id));
    }

    @GetMapping("/recommendations/{id}/decisions")
    @Operation(summary = "Append-only human decision ledger for recommendation")
    public ResponseEntity<?> listRecommendationDecisions(@PathVariable String id) {
        return ResponseEntity.ok(vendorRecommendationService.listDecisions(id));
    }

    @GetMapping("/recommendations/{id}/decision-events")
    @Operation(summary = "Decision event outbox for recommendation (integration feed)")
    public ResponseEntity<?> listRecommendationDecisionEvents(@PathVariable String id) {
        return ResponseEntity.ok(vendorRecommendationService.listDecisionEvents(id));
    }

    @GetMapping("/recommendations/decision-events/unpublished")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'SOURCING_LEAD')")
    @Operation(summary = "Unpublished recommendation decision events")
    public ResponseEntity<?> listUnpublishedDecisionEvents() {
        return ResponseEntity.ok(vendorRecommendationService.listUnpublishedEvents());
    }

    @PostMapping("/recommendations/decision-events/{eventId}/published")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'SOURCING_LEAD')")
    @Operation(summary = "Mark decision event published after successful integration delivery")
    public ResponseEntity<?> markDecisionEventPublished(@PathVariable String eventId) {
        return ResponseEntity.ok(vendorRecommendationService.markEventPublished(eventId));
    }

    @PostMapping("/recommendations/decision-events/dispatch")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'SOURCING_LEAD')")
    @Operation(summary = "Dispatch unpublished decision events to configured webhook")
    public ResponseEntity<?> dispatchDecisionEvents() {
        return ResponseEntity.ok(decisionWebhookService.dispatchUnpublished());
    }

    @PostMapping("/recommendations/decision-events/{eventId}/deliver")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'SOURCING_LEAD')")
    @Operation(summary = "Deliver one decision event via webhook")
    public ResponseEntity<?> deliverDecisionEvent(@PathVariable String eventId) {
        return ResponseEntity.ok(decisionWebhookService.deliverEvent(eventId));
    }

    @GetMapping("/recommendations/decision-events/{eventId}/deliveries")
    @Operation(summary = "Webhook delivery attempts for a decision event")
    public ResponseEntity<?> listDecisionEventDeliveries(@PathVariable String eventId) {
        return ResponseEntity.ok(decisionWebhookService.listDeliveries(eventId));
    }


    @PostMapping("/recommendations/{id}/submit")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Submit recommendation for human decision")
    public ResponseEntity<?> submitRecommendation(@PathVariable String id) {
        return ResponseEntity.ok(vendorRecommendationService.submit(id));
    }

    @PostMapping("/recommendations/{id}/approve")
    @PreAuthorize("hasAnyRole('SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "HUMAN approve recommended supplier")
    public ResponseEntity<?> approveRecommendation(@PathVariable String id,
            @RequestBody(required = false) com.automotive.costing.vendorselection.service.VendorRecommendationService.DecisionRequest request) {
        return ResponseEntity.ok(vendorRecommendationService.approve(id, request));
    }

    @PostMapping("/recommendations/{id}/reject")
    @PreAuthorize("hasAnyRole('SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "HUMAN reject recommendation")
    public ResponseEntity<?> rejectRecommendation(@PathVariable String id,
            @RequestBody(required = false) com.automotive.costing.vendorselection.service.VendorRecommendationService.DecisionRequest request) {
        return ResponseEntity.ok(vendorRecommendationService.reject(id, request));
    }

    @PostMapping("/recommendations/{id}/override")
    @PreAuthorize("hasAnyRole('SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "HUMAN override with alternate supplier + mandatory reason")
    public ResponseEntity<?> overrideRecommendation(@PathVariable String id,
            @RequestBody com.automotive.costing.vendorselection.service.VendorRecommendationService.DecisionRequest request) {
        return ResponseEntity.ok(vendorRecommendationService.override(id, request));
    }

    @PostMapping("/questionnaire/questions")
    @PreAuthorize("hasAnyRole('SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Create or version a questionnaire question (prior versions retained for history)")
    public ResponseEntity<?> createQuestion(
            @RequestBody com.automotive.costing.vendorselection.service.QuestionnaireService.CreateQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionnaireService.createQuestion(request));
    }

    @GetMapping("/questionnaire/questions")
    @Operation(summary = "List active questionnaire questions (optional category filter)")
    public ResponseEntity<?> listQuestions(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String category) {
        if (category == null || category.isBlank()) {
            return ResponseEntity.ok(questionnaireService.listActiveQuestions());
        }
        try {
            var cat = com.automotive.costing.vendorselection.entity.QuestionCategory.valueOf(category.trim().toUpperCase());
            return ResponseEntity.ok(questionnaireService.listActiveQuestions(cat));
        } catch (IllegalArgumentException ex) {
            throw new com.automotive.costing.common.exception.BusinessRuleViolationException(
                    "Unknown question category: " + category, "VS-Q-10");
        }
    }

    @PostMapping("/questionnaire/responses")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'COSTING_ENGINEER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Submit supplier answer bound to question version")
    public ResponseEntity<?> submitAnswer(
            @RequestBody com.automotive.costing.vendorselection.service.QuestionnaireService.SubmitAnswerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionnaireService.submitAnswer(request));
    }

    @GetMapping("/questionnaire/suppliers/{supplierId}/responses")
    public ResponseEntity<?> listResponses(@PathVariable String supplierId) {
        return ResponseEntity.ok(questionnaireService.listResponses(supplierId));
    }

    @PostMapping("/questionnaire/responses/{responseId}/verify")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'QUALITY_ENGINEER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Verify, fail, or waive questionnaire response evidence (WAIVED requires reason)")
    public ResponseEntity<?> verifyQuestionnaireEvidence(@PathVariable String responseId,
            @RequestBody com.automotive.costing.vendorselection.service.QuestionnaireService.VerifyEvidenceRequest request) {
        return ResponseEntity.ok(questionnaireService.verifyEvidence(responseId, request));
    }


    @PostMapping("/questionnaire/suppliers/{supplierId}/evaluate")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'COSTING_ENGINEER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Evaluate supplier against active questionnaire bank (mandatory gates + weighted score snapshot)")
    public ResponseEntity<?> evaluateQuestionnaire(@PathVariable String supplierId,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(questionnaireService.evaluateSupplier(supplierId, notes));
    }

    @GetMapping("/questionnaire/suppliers/{supplierId}/evaluation")
    @Operation(summary = "Latest questionnaire evaluation snapshot for supplier")
    public ResponseEntity<?> latestQuestionnaireEvaluation(@PathVariable String supplierId) {
        return ResponseEntity.ok(questionnaireService.latestEvaluation(supplierId));
    }

    @GetMapping("/questionnaire/suppliers/{supplierId}/evaluation/history")
    @Operation(summary = "Questionnaire evaluation history for supplier")
    public ResponseEntity<?> questionnaireEvaluationHistory(@PathVariable String supplierId) {
        return ResponseEntity.ok(questionnaireService.evaluationHistory(supplierId));
    }

    @PostMapping("/comparisons")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'COSTING_ENGINEER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Side-by-side supplier comparison (decision support, not approval)")
    public ResponseEntity<?> compare(
            @RequestBody com.automotive.costing.vendorselection.service.SupplierComparisonService.CompareRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierComparisonService.compareAndPersist(request));
    }

    @GetMapping("/comparisons")
    @Operation(summary = "List comparison snapshots (optional bomNodeId or programId; else recent 50)")
    public ResponseEntity<?> listComparisons(
            @RequestParam(required = false) String bomNodeId,
            @RequestParam(required = false) String programId) {
        if (bomNodeId != null && !bomNodeId.isBlank()) {
            return ResponseEntity.ok(supplierComparisonService.listByBomNode(bomNodeId.trim()));
        }
        if (programId != null && !programId.isBlank()) {
            return ResponseEntity.ok(supplierComparisonService.listByProgram(programId.trim()));
        }
        return ResponseEntity.ok(supplierComparisonService.listRecent());
    }

    @GetMapping("/comparisons/{id}")
    public ResponseEntity<?> getComparison(@PathVariable String id) {
        return ResponseEntity.ok(supplierComparisonService.get(id));
    }

    @PostMapping("/suppliers/{supplierId}/qualification/initialize")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Initialize supplier qualification at DISCOVERED")
    public ResponseEntity<?> initQualification(@PathVariable String supplierId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierQualificationService.initialize(supplierId));
    }

    @PostMapping("/qualification/transitions")
    @PreAuthorize("hasAnyRole('SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Advance qualification lifecycle (APPROVED requires human reason + approved recommendation for same supplier)")
    public ResponseEntity<?> transition(
            @RequestBody com.automotive.costing.vendorselection.service.SupplierQualificationService.TransitionRequest request) {
        return ResponseEntity.ok(supplierQualificationService.transition(request));
    }

    @GetMapping("/suppliers/{supplierId}/qualification")
    public ResponseEntity<?> currentQualification(@PathVariable String supplierId) {
        return ResponseEntity.ok(supplierQualificationService.current(supplierId));
    }

    @GetMapping("/suppliers/{supplierId}/qualification/history")
    public ResponseEntity<?> qualificationHistory(@PathVariable String supplierId) {
        return ResponseEntity.ok(supplierQualificationService.history(supplierId));
    }

    @GetMapping("/suppliers/{supplierId}/qualification/approval-checks")
    @Operation(summary = "Audit of APPROVED-gate recommendation checks for supplier")
    public ResponseEntity<?> qualificationApprovalChecks(@PathVariable String supplierId) {
        return ResponseEntity.ok(supplierQualificationService.listApprovalChecks(supplierId));
    }

    @GetMapping("/suppliers/{supplierId}/lifecycle-operation-blocks")
    @Operation(summary = "Attempts blocked by BLOCKED/SUSPENDED lifecycle (scorecard/TCO)")
    public ResponseEntity<?> lifecycleOperationBlocks(@PathVariable String supplierId) {
        return ResponseEntity.ok(supplierLifecycleGuard.listBlocks(supplierId));
    }

    @GetMapping("/suppliers/{supplierId}/qualification/lifecycle-events")
    @Operation(summary = "Qualification lifecycle event outbox for supplier")
    public ResponseEntity<?> qualificationLifecycleEvents(@PathVariable String supplierId) {
        return ResponseEntity.ok(supplierQualificationService.listLifecycleEvents(supplierId));
    }

    @GetMapping("/qualification/lifecycle-events/unpublished")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'SOURCING_LEAD')")
    @Operation(summary = "Unpublished qualification lifecycle events")
    public ResponseEntity<?> unpublishedQualificationLifecycleEvents() {
        return ResponseEntity.ok(supplierQualificationService.listUnpublishedLifecycleEvents());
    }

    @PostMapping("/qualification/lifecycle-events/{eventId}/published")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'SOURCING_LEAD')")
    @Operation(summary = "Mark qualification lifecycle event published after delivery")
    public ResponseEntity<?> markQualificationLifecycleEventPublished(@PathVariable String eventId) {
        return ResponseEntity.ok(supplierQualificationService.markLifecycleEventPublished(eventId));
    }

    @PostMapping("/risks")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Register supplier risk (separate from scorecard score)")
    public ResponseEntity<?> createRisk(
            @RequestBody com.automotive.costing.vendorselection.service.SupplierRiskService.CreateRiskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierRiskService.create(request));
    }

    @PostMapping("/risks/{riskId}/status")
    @PreAuthorize("hasAnyRole('SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Update risk status (ACCEPTED requires rationale)")
    public ResponseEntity<?> updateRiskStatus(@PathVariable String riskId,
            @RequestBody com.automotive.costing.vendorselection.service.SupplierRiskService.UpdateRiskStatusRequest request) {
        return ResponseEntity.ok(supplierRiskService.updateStatus(riskId, request));
    }

    @GetMapping("/suppliers/{supplierId}/risks")
    @Operation(summary = "List supplier risks (optional status and severity filters)")
    public ResponseEntity<?> listRisks(
            @PathVariable String supplierId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity) {
        com.automotive.costing.vendorselection.entity.RiskStatus st = null;
        com.automotive.costing.vendorselection.entity.RiskSeverity sev = null;
        try {
            if (status != null && !status.isBlank()) {
                st = com.automotive.costing.vendorselection.entity.RiskStatus.valueOf(status.trim().toUpperCase());
            }
            if (severity != null && !severity.isBlank()) {
                sev = com.automotive.costing.vendorselection.entity.RiskSeverity.valueOf(severity.trim().toUpperCase());
            }
        } catch (IllegalArgumentException ex) {
            throw new com.automotive.costing.common.exception.BusinessRuleViolationException(
                    "Invalid risk status or severity", "VS-RISK-10");
        }
        return ResponseEntity.ok(supplierRiskService.listBySupplier(supplierId, st, sev));
    }

    @GetMapping("/suppliers/{supplierId}/risks/summary")
    @Operation(summary = "Open/mitigating risk summary for supplier")
    public ResponseEntity<?> riskSummary(@PathVariable String supplierId) {
        return ResponseEntity.ok(supplierRiskService.summary(supplierId));
    }

    // ——— Policy & compliance evidence (V43) ———

    @PostMapping("/policies")
    @PreAuthorize("hasAnyRole('PURCHASING_BUYER', 'SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Register supplier policy/compliance evidence (starts PENDING; declaration is not verified)")
    public ResponseEntity<?> addPolicy(
            @Valid @RequestBody com.automotive.costing.vendorselection.dto.VendorSelectionDtos.CreatePolicyEvidenceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierEvidenceService.addPolicy(request));
    }

    @PostMapping("/policies/{policyId}/verify")
    @PreAuthorize("hasAnyRole('SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Verify, fail, expire, or waive policy evidence (WAIVED requires rationale)")
    public ResponseEntity<?> verifyPolicy(@PathVariable String policyId,
            @Valid @RequestBody com.automotive.costing.vendorselection.dto.VendorSelectionDtos.VerifyPolicyRequest request) {
        return ResponseEntity.ok(supplierEvidenceService.verifyPolicy(policyId, request));
    }

    @GetMapping("/suppliers/{supplierId}/policies")
    @Operation(summary = "List policy/compliance evidence (optional evidenceStatus / policyType)")
    public ResponseEntity<?> listPolicies(
            @PathVariable String supplierId,
            @RequestParam(required = false) String evidenceStatus,
            @RequestParam(required = false) String policyType) {
        com.automotive.costing.vendorselection.entity.EvidenceStatus es = null;
        com.automotive.costing.vendorselection.entity.PolicyType pt = null;
        try {
            if (evidenceStatus != null && !evidenceStatus.isBlank()) {
                es = com.automotive.costing.vendorselection.entity.EvidenceStatus.valueOf(evidenceStatus.trim().toUpperCase());
            }
            if (policyType != null && !policyType.isBlank()) {
                pt = com.automotive.costing.vendorselection.entity.PolicyType.valueOf(policyType.trim().toUpperCase());
            }
        } catch (IllegalArgumentException ex) {
            throw new com.automotive.costing.common.exception.BusinessRuleViolationException(
                    "Invalid evidenceStatus or policyType", "VS-POL-10");
        }
        return ResponseEntity.ok(supplierEvidenceService.listPolicies(supplierId, es, pt));
    }

    @GetMapping("/suppliers/{supplierId}/policies/summary")
    @Operation(summary = "Policy evidence summary counts for supplier")
    public ResponseEntity<?> policySummary(@PathVariable String supplierId) {
        return ResponseEntity.ok(supplierEvidenceService.policySummary(supplierId));
    }

    // ——— Evidence expiry (V44) ———

    @PostMapping("/evidence/expiry-run")
    @PreAuthorize("hasAnyRole('SOURCING_LEAD', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Mark past-expiry policy/warranty/guarantee evidence as EXPIRED")
    public ResponseEntity<?> runEvidenceExpiry(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate asOfDate) {
        return ResponseEntity.status(HttpStatus.CREATED).body(evidenceExpiryService.runExpiryPass(asOfDate));
    }

    @GetMapping("/evidence/expiry-runs")
    @Operation(summary = "Recent evidence expiry run history")
    public ResponseEntity<?> listExpiryRuns() {
        return ResponseEntity.ok(evidenceExpiryService.recentRuns());
    }

    @GetMapping("/evidence/expiry-job")
    @Operation(summary = "Evidence expiry scheduler job status")
    public ResponseEntity<?> evidenceExpiryJobStatus() {
        return ResponseEntity.ok(evidenceExpiryService.jobStatus());
    }

    @PostMapping("/evidence/expiry-job/enabled")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'SOURCING_LEAD', 'COSTING_MANAGER')")
    @Operation(summary = "Enable or disable scheduled evidence expiry job (DB flag)")
    public ResponseEntity<?> setEvidenceExpiryJobEnabled(@RequestParam boolean enabled) {
        return ResponseEntity.ok(evidenceExpiryService.setJobEnabled(enabled));
    }

    @PostMapping("/capabilities/expiry-run")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'SOURCING_LEAD', 'COSTING_MANAGER', 'MASTER_DATA_ADMIN')")
    @Operation(summary = "Manual capability expiry pass (ACTIVE past valid_to → EXPIRED)")
    public ResponseEntity<?> capabilityExpiryRun(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate asOfDate) {
        return ResponseEntity.status(HttpStatus.CREATED).body(capabilityExpiryService.runExpiryPass(asOfDate));
    }

    @GetMapping("/capabilities/expiry-runs")
    @Operation(summary = "Recent capability expiry runs")
    public ResponseEntity<?> capabilityExpiryRuns() {
        return ResponseEntity.ok(capabilityExpiryService.recentRuns());
    }

    @GetMapping("/capabilities/expiry-job")
    @Operation(summary = "Capability expiry scheduler job status")
    public ResponseEntity<?> capabilityExpiryJobStatus() {
        return ResponseEntity.ok(capabilityExpiryService.jobStatus());
    }

    @PostMapping("/capabilities/expiry-job/enabled")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'SOURCING_LEAD', 'COSTING_MANAGER')")
    @Operation(summary = "Enable or disable capability expiry job")
    public ResponseEntity<?> setCapabilityExpiryJobEnabled(@RequestParam boolean enabled) {
        return ResponseEntity.ok(capabilityExpiryService.setJobEnabled(enabled));
    }

}
