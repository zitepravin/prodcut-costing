package com.automotive.costing.vendorselection.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.dto.VendorSelectionDtos.*;
import com.automotive.costing.vendorselection.entity.*;
import com.automotive.costing.vendorselection.repository.*;
import com.automotive.costing.vendorselection.engine.WarrantyClaimStatusTransitionEngine;
import com.automotive.costing.vendorselection.engine.EvidenceStatusTransitionEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupplierEvidenceService {

    private final SupplierQualityMetricRepository qualityRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final SupplierDeliveryMetricRepository deliveryRepository;
    private final SupplierWarrantyTermRepository warrantyRepository;
    private final SupplierWarrantyClaimRepository warrantyClaimRepository;
    private final SupplierGuaranteeTermRepository guaranteeRepository;
    private final SupplierGuaranteeEventRepository guaranteeEventRepository;
    private final SupplierCapabilityRepository capabilityRepository;
    private final SupplierAfterSalesMetricRepository afterSalesRepository;
    private final SupplierPerformanceFeedbackRepository performanceFeedbackRepository;
    private final SupplierPolicyEvidenceRepository policyRepository;
    private final SupplierMasterService supplierMasterService;
    private final AuditService auditService;
    private final WarrantyClaimStatusTransitionEngine claimStatusTransitionEngine;
    private final EvidenceStatusTransitionEngine evidenceStatusTransitionEngine;

    @Transactional
    public QualityMetricResponse addQuality(CreateQualityMetricRequest r) {
        supplierMasterService.require(r.supplierId());
        if (r.periodEnd().isBefore(r.periodStart())) {
            throw new BusinessRuleViolationException("periodEnd must be on or after periodStart", "VS-B2-01");
        }
        SupplierQualityMetric m = new SupplierQualityMetric();
        m.setMetricId(UUID.randomUUID().toString());
        m.setCustomerId(tenantAccessGuard.requireCustomerId());
        m.setSupplierId(r.supplierId());
        m.setMetricType(r.metricType());
        m.setPeriodStart(r.periodStart());
        m.setPeriodEnd(r.periodEnd());
        m.setMetricValue(r.metricValue());
        m.setUnit(r.unit());
        m.setSampleSize(r.sampleSize());
        m.setEvidenceStatus(EvidenceStatus.PENDING);
        m.setEvidenceRef(r.evidenceRef());
        m.setNotes(r.notes());
        qualityRepository.save(m);
        auditService.recordCreate("SupplierQualityMetric", m.getMetricId(), r.metricType().name());
        return toQuality(m);
    }

    @Transactional
    public DeliveryMetricResponse addDelivery(CreateDeliveryMetricRequest r) {
        supplierMasterService.require(r.supplierId());
        if (r.periodEnd().isBefore(r.periodStart())) {
            throw new BusinessRuleViolationException("periodEnd must be on or after periodStart", "VS-B2-01");
        }
        SupplierDeliveryMetric m = new SupplierDeliveryMetric();
        m.setMetricId(UUID.randomUUID().toString());
        m.setCustomerId(tenantAccessGuard.requireCustomerId());
        m.setSupplierId(r.supplierId());
        m.setPeriodStart(r.periodStart());
        m.setPeriodEnd(r.periodEnd());
        m.setOtdPercent(r.otdPercent());
        m.setOtifPercent(r.otifPercent());
        m.setLateDeliveryCount(r.lateDeliveryCount());
        m.setAvgDelayDays(r.avgDelayDays());
        m.setEvidenceStatus(EvidenceStatus.PENDING);
        m.setEvidenceRef(r.evidenceRef());
        m.setNotes(r.notes());
        deliveryRepository.save(m);
        auditService.recordCreate("SupplierDeliveryMetric", m.getMetricId(), r.supplierId());
        return toDelivery(m);
    }

    @Transactional
    public WarrantyResponse addWarranty(CreateWarrantyRequest r) {
        supplierMasterService.require(r.supplierId());
        SupplierWarrantyTerm w = new SupplierWarrantyTerm();
        w.setWarrantyId(UUID.randomUUID().toString());
        w.setCustomerId(tenantAccessGuard.requireCustomerId());
        w.setSupplierId(r.supplierId());
        w.setDurationMonths(r.durationMonths());
        w.setCoverageSummary(r.coverageSummary());
        w.setExclusions(r.exclusions());
        w.setClaimSlaDays(r.claimSlaDays());
        w.setRemedy(r.remedy());
        w.setEffectiveFrom(r.effectiveFrom());
        w.setEffectiveTo(r.effectiveTo());
        w.setClaimRate(r.claimRate());
        w.setHistoricalCost(r.historicalCost());
        w.setCurrencyCode(r.currencyCode());
        w.setEvidenceStatus(EvidenceStatus.PENDING);
        w.setEvidenceRef(r.evidenceRef());
        w.setNotes(r.notes());
        warrantyRepository.save(w);
        auditService.recordCreate("SupplierWarrantyTerm", w.getWarrantyId(), r.supplierId());
        return toWarranty(w);
    }

    @Transactional
    public GuaranteeResponse addGuarantee(CreateGuaranteeRequest r) {
        supplierMasterService.require(r.supplierId());
        SupplierGuaranteeTerm g = new SupplierGuaranteeTerm();
        g.setGuaranteeId(UUID.randomUUID().toString());
        g.setCustomerId(tenantAccessGuard.requireCustomerId());
        g.setSupplierId(r.supplierId());
        g.setGuaranteeType(r.guaranteeType());
        g.setDescription(r.description());
        g.setPeriodMonths(r.periodMonths());
        g.setConditionsText(r.conditionsText());
        g.setFailureConsequence(r.failureConsequence());
        g.setEffectiveFrom(r.effectiveFrom());
        g.setEffectiveTo(r.effectiveTo());
        g.setEvidenceStatus(EvidenceStatus.PENDING);
        g.setEvidenceRef(r.evidenceRef());
        g.setNotes(r.notes());
        guaranteeRepository.save(g);
        auditService.recordCreate("SupplierGuaranteeTerm", g.getGuaranteeId(), r.guaranteeType().name());
        return toGuarantee(g);
    }

    @Transactional
    public QualityMetricResponse verifyQuality(String metricId, VerifyEvidenceRequest req) {
        String customerId = tenantAccessGuard.requireCustomerId();
        SupplierQualityMetric m = qualityRepository.findByCustomerIdAndMetricId(customerId, metricId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupplierQualityMetric", metricId));
        tenantAccessGuard.assertOwns(m.getCustomerId(), "SupplierQualityMetric", metricId);
        EvidenceStatus previous = m.getEvidenceStatus();
        applyVerify(m, req);
        qualityRepository.save(m);
        auditService.recordVerify("SupplierQualityMetric", metricId,
                previous != null ? previous.name() : null, req.evidenceStatus().name());
        return toQuality(m);
    }

    @Transactional
    public DeliveryMetricResponse verifyDelivery(String metricId, VerifyEvidenceRequest req) {
        String customerId = tenantAccessGuard.requireCustomerId();
        SupplierDeliveryMetric m = deliveryRepository.findByCustomerIdAndMetricId(customerId, metricId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupplierDeliveryMetric", metricId));
        tenantAccessGuard.assertOwns(m.getCustomerId(), "SupplierDeliveryMetric", metricId);
        EvidenceStatus previous = m.getEvidenceStatus();
        applyVerify(m, req);
        deliveryRepository.save(m);
        auditService.recordVerify("SupplierDeliveryMetric", metricId,
                previous != null ? previous.name() : null, req.evidenceStatus().name());
        return toDelivery(m);
    }


    @Transactional
    public WarrantyClaimResponse addWarrantyClaim(CreateWarrantyClaimRequest r) {
        supplierMasterService.require(r.supplierId());
        if (r.warrantyId() != null && !r.warrantyId().isBlank()) {
            warrantyRepository.findByCustomerIdAndWarrantyId(tenantAccessGuard.requireCustomerId(), r.warrantyId())
                    .orElseThrow(() -> ResourceNotFoundException.of("SupplierWarrantyTerm", r.warrantyId()));
        }
        if (r.claimCost() != null && r.claimCost().signum() < 0) {
            throw new BusinessRuleViolationException("claimCost must not be negative", "VS-WC-01");
        }
        SupplierWarrantyClaim c = new SupplierWarrantyClaim();
        c.setClaimId(UUID.randomUUID().toString());
        c.setCustomerId(tenantAccessGuard.requireCustomerId());
        c.setSupplierId(r.supplierId());
        c.setWarrantyId(r.warrantyId());
        c.setClaimDate(r.claimDate());
        c.setClaimNumber(r.claimNumber());
        c.setPartNumber(r.partNumber());
        c.setClaimCost(r.claimCost());
        c.setCurrencyCode(r.currencyCode());
        c.setClaimStatus(r.claimStatus() != null ? r.claimStatus() : WarrantyClaimStatus.OPEN);
        c.setAcceptedFlag(r.acceptedFlag());
        c.setResolutionDays(r.resolutionDays());
        c.setResolutionSummary(r.resolutionSummary());
        c.setEvidenceStatus(EvidenceStatus.PENDING);
        c.setEvidenceRef(r.evidenceRef());
        c.setNotes(r.notes());
        warrantyClaimRepository.save(c);
        auditService.recordCreate("SupplierWarrantyClaim", c.getClaimId(), r.supplierId());
        return toWarrantyClaim(c);
    }

    @Transactional
    public WarrantyClaimResponse updateWarrantyClaimStatus(String claimId, UpdateWarrantyClaimStatusRequest req) {
        String customerId = tenantAccessGuard.requireCustomerId();
        SupplierWarrantyClaim c = warrantyClaimRepository.findByCustomerIdAndClaimId(customerId, claimId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupplierWarrantyClaim", claimId));
        tenantAccessGuard.assertOwns(c.getCustomerId(), "SupplierWarrantyClaim", claimId);
        claimStatusTransitionEngine.assertTransitionAllowed(c.getClaimStatus(), req.claimStatus());
        WarrantyClaimStatus previous = c.getClaimStatus();
        c.setClaimStatus(req.claimStatus());
        if (req.acceptedFlag() != null) {
            c.setAcceptedFlag(req.acceptedFlag());
        } else if (req.claimStatus() == WarrantyClaimStatus.ACCEPTED) {
            c.setAcceptedFlag(true);
        } else if (req.claimStatus() == WarrantyClaimStatus.REJECTED) {
            c.setAcceptedFlag(false);
        }
        if (req.resolutionDays() != null) {
            c.setResolutionDays(req.resolutionDays());
        }
        if (req.resolutionSummary() != null) {
            c.setResolutionSummary(req.resolutionSummary());
        }
        if (req.notes() != null) {
            c.setNotes(req.notes());
        }
        warrantyClaimRepository.save(c);
        auditService.recordTransition("SupplierWarrantyClaim", claimId, previous.name(), req.claimStatus().name());
        return toWarrantyClaim(c);
    }

    @Transactional
    public WarrantyClaimResponse verifyWarrantyClaim(String claimId, VerifyEvidenceRequest req) {
        String customerId = tenantAccessGuard.requireCustomerId();
        SupplierWarrantyClaim c = warrantyClaimRepository.findByCustomerIdAndClaimId(customerId, claimId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupplierWarrantyClaim", claimId));
        tenantAccessGuard.assertOwns(c.getCustomerId(), "SupplierWarrantyClaim", claimId);
        EvidenceStatus previousEvidence = c.getEvidenceStatus();
        evidenceStatusTransitionEngine.assertTransitionAllowed(previousEvidence, req.evidenceStatus());
        c.setEvidenceStatus(req.evidenceStatus());
        if (req.evidenceRef() != null) c.setEvidenceRef(req.evidenceRef());
        c.setVerifiedBy(currentUserId());
        c.setVerifiedAt(Instant.now());
        warrantyClaimRepository.save(c);
        auditService.recordVerify("SupplierWarrantyClaim", claimId,
                previousEvidence != null ? previousEvidence.name() : null, req.evidenceStatus().name());
        return toWarrantyClaim(c);
    }


    @Transactional(readOnly = true)
    public java.util.List<WarrantyClaimResponse> listWarrantyClaims(
            String supplierId, WarrantyClaimStatus claimStatus, EvidenceStatus evidenceStatus) {
        supplierMasterService.require(supplierId);
        java.util.List<SupplierWarrantyClaim> rows;
        if (claimStatus != null && evidenceStatus != null) {
            rows = warrantyClaimRepository.findBySupplierIdAndClaimStatusAndEvidenceStatusOrderByClaimDateDesc(
                    supplierId, claimStatus, evidenceStatus);
        } else if (claimStatus != null) {
            rows = warrantyClaimRepository.findBySupplierIdAndClaimStatusOrderByClaimDateDesc(supplierId, claimStatus);
        } else if (evidenceStatus != null) {
            rows = warrantyClaimRepository.findBySupplierIdAndEvidenceStatusOrderByClaimDateDesc(supplierId, evidenceStatus);
        } else {
            rows = warrantyClaimRepository.findByCustomerIdAndSupplierIdOrderByClaimDateDesc(tenantAccessGuard.requireCustomerId(), supplierId);
        }
        return rows.stream().map(this::toWarrantyClaim).toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<GuaranteeEventResponse> listGuaranteeEvents(String supplierId, Boolean unresolvedOnly) {
        return listGuaranteeEvents(supplierId, unresolvedOnly, null);
    }

    @Transactional(readOnly = true)
    public java.util.List<GuaranteeEventResponse> listGuaranteeEvents(
            String supplierId, Boolean unresolvedOnly, EvidenceStatus evidenceStatus) {
        supplierMasterService.require(supplierId);
        java.util.List<SupplierGuaranteeEvent> rows;
        if (evidenceStatus != null && unresolvedOnly != null) {
            boolean resolvedFlag = Boolean.FALSE.equals(unresolvedOnly); // unresolvedOnly=true → resolved false
            rows = guaranteeEventRepository.findBySupplierIdAndResolvedFlagAndEvidenceStatusOrderByEventDateDesc(
                    supplierId, resolvedFlag, evidenceStatus);
        } else if (evidenceStatus != null) {
            rows = guaranteeEventRepository.findBySupplierIdAndEvidenceStatusOrderByEventDateDesc(supplierId, evidenceStatus);
        } else if (Boolean.TRUE.equals(unresolvedOnly)) {
            rows = guaranteeEventRepository.findBySupplierIdAndResolvedFlagOrderByEventDateDesc(supplierId, false);
        } else if (Boolean.FALSE.equals(unresolvedOnly)) {
            rows = guaranteeEventRepository.findBySupplierIdAndResolvedFlagOrderByEventDateDesc(supplierId, true);
        } else {
            rows = guaranteeEventRepository.findByCustomerIdAndSupplierIdOrderByEventDateDesc(tenantAccessGuard.requireCustomerId(), supplierId);
        }
        return rows.stream().map(this::toGuaranteeEvent).toList();
    }

    public WarrantyClaimSummaryResponse warrantyClaimSummary(String supplierId) {
        supplierMasterService.require(supplierId);
        var claims = warrantyClaimRepository.findByCustomerIdAndSupplierIdOrderByClaimDateDesc(tenantAccessGuard.requireCustomerId(), supplierId);
        long open = claims.stream().filter(c -> c.getClaimStatus() == WarrantyClaimStatus.OPEN).count();
        long accepted = claims.stream().filter(c -> c.getClaimStatus() == WarrantyClaimStatus.ACCEPTED).count();
        long rejected = claims.stream().filter(c -> c.getClaimStatus() == WarrantyClaimStatus.REJECTED).count();
        long closed = claims.stream().filter(c -> c.getClaimStatus() == WarrantyClaimStatus.CLOSED).count();
        long withdrawn = claims.stream().filter(c -> c.getClaimStatus() == WarrantyClaimStatus.WITHDRAWN).count();
        java.math.BigDecimal totalCost = claims.stream().map(SupplierWarrantyClaim::getClaimCost)
                .filter(java.util.Objects::nonNull).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        String currency = claims.stream().map(SupplierWarrantyClaim::getCurrencyCode)
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);
        return new WarrantyClaimSummaryResponse(supplierId, claims.size(), open, accepted, rejected, closed, withdrawn, totalCost, currency,
                claims.stream().map(this::toWarrantyClaim).toList());
    }

    @Transactional
    public GuaranteeEventResponse addGuaranteeEvent(CreateGuaranteeEventRequest r) {
        supplierMasterService.require(r.supplierId());
        if (r.guaranteeId() != null && !r.guaranteeId().isBlank()) {
            guaranteeRepository.findByCustomerIdAndGuaranteeId(tenantAccessGuard.requireCustomerId(), r.guaranteeId())
                    .orElseThrow(() -> ResourceNotFoundException.of("SupplierGuaranteeTerm", r.guaranteeId()));
        }
        if (r.penaltyAmount() != null && r.penaltyAmount().signum() < 0) {
            throw new BusinessRuleViolationException("penaltyAmount must not be negative", "VS-GE-01");
        }
        SupplierGuaranteeEvent e = new SupplierGuaranteeEvent();
        e.setEventId(UUID.randomUUID().toString());
        e.setCustomerId(tenantAccessGuard.requireCustomerId());
        e.setSupplierId(r.supplierId());
        e.setGuaranteeId(r.guaranteeId());
        e.setEventDate(r.eventDate());
        e.setEventType(r.eventType());
        e.setDescription(r.description());
        e.setConsequenceApplied(r.consequenceApplied());
        e.setPenaltyAmount(r.penaltyAmount());
        e.setCurrencyCode(r.currencyCode());
        e.setResolvedFlag(r.resolvedFlag());
        e.setResolutionSummary(r.resolutionSummary());
        e.setEvidenceStatus(EvidenceStatus.PENDING);
        e.setEvidenceRef(r.evidenceRef());
        e.setNotes(r.notes());
        guaranteeEventRepository.save(e);
        auditService.recordCreate("SupplierGuaranteeEvent", e.getEventId(), r.eventType().name());
        return toGuaranteeEvent(e);
    }

    @Transactional
    public GuaranteeEventResponse verifyGuaranteeEvent(String eventId, VerifyEvidenceRequest req) {
        String customerId = tenantAccessGuard.requireCustomerId();
        SupplierGuaranteeEvent e = guaranteeEventRepository.findByCustomerIdAndEventId(customerId, eventId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupplierGuaranteeEvent", eventId));
        tenantAccessGuard.assertOwns(e.getCustomerId(), "SupplierGuaranteeEvent", eventId);
        EvidenceStatus previous = e.getEvidenceStatus();
        evidenceStatusTransitionEngine.assertTransitionAllowed(previous, req.evidenceStatus());
        e.setEvidenceStatus(req.evidenceStatus());
        if (req.evidenceRef() != null) e.setEvidenceRef(req.evidenceRef());
        e.setVerifiedBy(currentUserId());
        e.setVerifiedAt(Instant.now());
        guaranteeEventRepository.save(e);
        auditService.recordVerify("SupplierGuaranteeEvent", eventId,
                previous != null ? previous.name() : null, req.evidenceStatus().name());
        return toGuaranteeEvent(e);
    }

    @Transactional
    public AfterSalesMetricResponse addAfterSales(CreateAfterSalesMetricRequest r) {
        supplierMasterService.require(r.supplierId());
        if (r.periodEnd().isBefore(r.periodStart())) {
            throw new BusinessRuleViolationException("periodEnd must be on or after periodStart", "VS-AS-01");
        }
        if (r.unresolvedComplaints() != null && r.unresolvedComplaints() < 0) {
            throw new BusinessRuleViolationException("unresolvedComplaints must not be negative", "VS-AS-02");
        }
        SupplierAfterSalesMetric m = new SupplierAfterSalesMetric();
        m.setMetricId(UUID.randomUUID().toString());
        m.setCustomerId(tenantAccessGuard.requireCustomerId());
        m.setSupplierId(r.supplierId());
        m.setPeriodStart(r.periodStart());
        m.setPeriodEnd(r.periodEnd());
        m.setResponseTimeHours(r.responseTimeHours());
        m.setResolutionTimeHours(r.resolutionTimeHours());
        m.setEmergencySupportFlag(r.emergencySupportFlag());
        m.setFieldSupportFlag(r.fieldSupportFlag());
        m.setSparePartsAvailable(r.sparePartsAvailable());
        m.setLocalSupportFlag(r.localSupportFlag());
        m.setUnresolvedComplaints(r.unresolvedComplaints());
        m.setAvgClosureDays(r.avgClosureDays());
        m.setServiceSlaMetPercent(r.serviceSlaMetPercent());
        m.setEvidenceStatus(EvidenceStatus.PENDING);
        m.setEvidenceRef(r.evidenceRef());
        m.setNotes(r.notes());
        afterSalesRepository.save(m);
        auditService.recordCreate("SupplierAfterSalesMetric", m.getMetricId(), r.supplierId());
        return toAfterSales(m);
    }

    @Transactional
    public AfterSalesMetricResponse verifyAfterSales(String metricId, VerifyEvidenceRequest req) {
        String customerId = tenantAccessGuard.requireCustomerId();
        SupplierAfterSalesMetric m = afterSalesRepository.findByCustomerIdAndMetricId(customerId, metricId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupplierAfterSalesMetric", metricId));
        tenantAccessGuard.assertOwns(m.getCustomerId(), "SupplierAfterSalesMetric", metricId);
        EvidenceStatus previous = m.getEvidenceStatus();
        evidenceStatusTransitionEngine.assertTransitionAllowed(previous, req.evidenceStatus());
        m.setEvidenceStatus(req.evidenceStatus());
        if (req.evidenceRef() != null) m.setEvidenceRef(req.evidenceRef());
        m.setVerifiedBy(currentUserId());
        m.setVerifiedAt(Instant.now());
        afterSalesRepository.save(m);
        auditService.recordVerify("SupplierAfterSalesMetric", metricId,
                previous != null ? previous.name() : null, req.evidenceStatus().name());
        return toAfterSales(m);
    }

    @Transactional
    public WarrantyResponse verifyWarranty(String warrantyId, VerifyEvidenceRequest req) {
        String customerId = tenantAccessGuard.requireCustomerId();
        SupplierWarrantyTerm w = warrantyRepository.findByCustomerIdAndWarrantyId(customerId, warrantyId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupplierWarrantyTerm", warrantyId));
        tenantAccessGuard.assertOwns(w.getCustomerId(), "SupplierWarrantyTerm", warrantyId);
        EvidenceStatus previous = w.getEvidenceStatus();
        applyVerify(w, req);
        warrantyRepository.save(w);
        auditService.recordVerify("SupplierWarrantyTerm", warrantyId,
                previous != null ? previous.name() : null, req.evidenceStatus().name());
        return toWarranty(w);
    }

    @Transactional
    public GuaranteeResponse verifyGuarantee(String guaranteeId, VerifyEvidenceRequest req) {
        String customerId = tenantAccessGuard.requireCustomerId();
        SupplierGuaranteeTerm g = guaranteeRepository.findByCustomerIdAndGuaranteeId(customerId, guaranteeId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupplierGuaranteeTerm", guaranteeId));
        tenantAccessGuard.assertOwns(g.getCustomerId(), "SupplierGuaranteeTerm", guaranteeId);
        EvidenceStatus previous = g.getEvidenceStatus();
        applyVerify(g, req);
        guaranteeRepository.save(g);
        auditService.recordVerify("SupplierGuaranteeTerm", guaranteeId,
                previous != null ? previous.name() : null, req.evidenceStatus().name());
        return toGuarantee(g);
    }

    @Transactional
    public PolicyEvidenceResponse addPolicy(CreatePolicyEvidenceRequest r) {
        supplierMasterService.require(r.supplierId());
        if (r.policyTitle() == null || r.policyTitle().isBlank()) {
            throw new BusinessRuleViolationException("policyTitle is required", "VS-POL-01");
        }
        if (r.effectiveFrom() != null && r.effectiveTo() != null && r.effectiveTo().isBefore(r.effectiveFrom())) {
            throw new BusinessRuleViolationException("effectiveTo must be on or after effectiveFrom", "VS-POL-02");
        }
        SupplierPolicyEvidence p = new SupplierPolicyEvidence();
        p.setPolicyId(UUID.randomUUID().toString());
        p.setCustomerId(tenantAccessGuard.requireCustomerId());
        p.setSupplierId(r.supplierId());
        p.setPolicyType(r.policyType());
        p.setPolicyTitle(r.policyTitle());
        p.setDescription(r.description());
        p.setPolicyVersion(r.policyVersion());
        p.setEffectiveFrom(r.effectiveFrom());
        p.setEffectiveTo(r.effectiveTo());
        p.setExpiryDate(r.expiryDate());
        p.setDeclarationOnly(r.declarationOnly());
        // Declaration-only records remain PENDING until independently verified; never auto-VERIFIED
        p.setEvidenceStatus(EvidenceStatus.PENDING);
        p.setEvidenceRef(r.evidenceRef());
        p.setNotes(r.notes());
        policyRepository.save(p);
        auditService.recordCreate("SupplierPolicyEvidence", p.getPolicyId(), r.policyType().name());
        return toPolicy(p);
    }

    @Transactional
    public PolicyEvidenceResponse verifyPolicy(String policyId, VerifyPolicyRequest req) {
        String customerId = tenantAccessGuard.requireCustomerId();
        SupplierPolicyEvidence p = policyRepository.findByCustomerIdAndPolicyId(customerId, policyId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupplierPolicyEvidence", policyId));
        tenantAccessGuard.assertOwns(p.getCustomerId(), "SupplierPolicyEvidence", policyId);
        if (req.evidenceStatus() == EvidenceStatus.WAIVED) {
            if (req.waiverReason() == null || req.waiverReason().isBlank()) {
                throw new BusinessRuleViolationException(
                        "WAIVED status requires waiverReason for audit", "VS-POL-04");
            }
            p.setWaiverReason(req.waiverReason());
        }
        if (req.evidenceStatus() == EvidenceStatus.VERIFIED && p.isDeclarationOnly()
                && (req.evidenceRef() == null || req.evidenceRef().isBlank())
                && (p.getEvidenceRef() == null || p.getEvidenceRef().isBlank())) {
            throw new BusinessRuleViolationException(
                    "Cannot VERIFIED a declaration-only policy without independent evidence reference",
                    "VS-POL-05");
        }
        EvidenceStatus previous = p.getEvidenceStatus();
        evidenceStatusTransitionEngine.assertTransitionAllowed(previous, req.evidenceStatus());
        p.setEvidenceStatus(req.evidenceStatus());
        if (req.evidenceRef() != null) {
            p.setEvidenceRef(req.evidenceRef());
        }
        p.setVerifiedBy(currentUserId());
        p.setVerifiedAt(Instant.now());
        policyRepository.save(p);
        auditService.recordVerify("SupplierPolicyEvidence", policyId,
                previous != null ? previous.name() : null, req.evidenceStatus().name());
        return toPolicy(p);
    }

    @Transactional(readOnly = true)
    public List<PolicyEvidenceResponse> listPolicies(String supplierId) {
        return listPolicies(supplierId, null, null);
    }

    @Transactional(readOnly = true)
    public List<PolicyEvidenceResponse> listPolicies(
            String supplierId, EvidenceStatus evidenceStatus, PolicyType policyType) {
        supplierMasterService.require(supplierId);
        java.util.List<SupplierPolicyEvidence> rows;
        if (evidenceStatus != null) {
            rows = policyRepository.findByCustomerIdAndSupplierIdAndEvidenceStatus(tenantAccessGuard.requireCustomerId(), supplierId, evidenceStatus);
        } else if (policyType != null) {
            rows = policyRepository.findByCustomerIdAndSupplierIdAndPolicyType(tenantAccessGuard.requireCustomerId(), supplierId, policyType);
        } else {
            rows = policyRepository.findByCustomerIdAndSupplierIdOrderByCreatedAtDesc(tenantAccessGuard.requireCustomerId(), supplierId);
        }
        return rows.stream().map(this::toPolicy).toList();
    }

    @Transactional(readOnly = true)
    public PolicySummaryResponse policySummary(String supplierId) {
        supplierMasterService.require(supplierId);
        List<SupplierPolicyEvidence> all = policyRepository.findByCustomerIdAndSupplierIdOrderByCreatedAtDesc(tenantAccessGuard.requireCustomerId(), supplierId);
        long verified = all.stream().filter(p -> p.getEvidenceStatus() == EvidenceStatus.VERIFIED).count();
        long pending = all.stream().filter(p -> p.getEvidenceStatus() == EvidenceStatus.PENDING).count();
        long failedOrExpired = all.stream()
                .filter(p -> p.getEvidenceStatus() == EvidenceStatus.FAILED
                        || p.getEvidenceStatus() == EvidenceStatus.EXPIRED)
                .count();
        long waived = all.stream().filter(p -> p.getEvidenceStatus() == EvidenceStatus.WAIVED).count();
        long declarationOnly = all.stream().filter(SupplierPolicyEvidence::isDeclarationOnly).count();
        return new PolicySummaryResponse(
                supplierId,
                all.size(),
                verified,
                pending,
                failedOrExpired,
                waived,
                declarationOnly,
                all.stream().map(this::toPolicy).toList());
    }

    

    @Transactional(readOnly = true)
    public EvidencePendingSummaryResponse evidencePendingSummary(String supplierId) {
        supplierMasterService.require(supplierId);
        long pendingPolicy = policyRepository.findByCustomerIdAndSupplierIdAndEvidenceStatus(tenantAccessGuard.requireCustomerId(), supplierId, EvidenceStatus.PENDING).size();
        long pendingQuality = qualityRepository.findByCustomerIdAndSupplierIdOrderByPeriodEndDesc(tenantAccessGuard.requireCustomerId(), supplierId).stream()
                .filter(m -> m.getEvidenceStatus() == EvidenceStatus.PENDING).count();
        long pendingDelivery = deliveryRepository.findByCustomerIdAndSupplierIdOrderByPeriodEndDesc(tenantAccessGuard.requireCustomerId(), supplierId).stream()
                .filter(m -> m.getEvidenceStatus() == EvidenceStatus.PENDING).count();
        long pendingClaims = warrantyClaimRepository.findByCustomerIdAndSupplierIdOrderByClaimDateDesc(tenantAccessGuard.requireCustomerId(), supplierId).stream()
                .filter(c -> c.getEvidenceStatus() == EvidenceStatus.PENDING).count();
        long pendingEvents = guaranteeEventRepository.findByCustomerIdAndSupplierIdOrderByEventDateDesc(tenantAccessGuard.requireCustomerId(), supplierId).stream()
                .filter(e -> e.getEvidenceStatus() == EvidenceStatus.PENDING).count();
        long pendingAs = afterSalesRepository.findByCustomerIdAndSupplierIdOrderByPeriodEndDesc(tenantAccessGuard.requireCustomerId(), supplierId).stream()
                .filter(m -> m.getEvidenceStatus() == EvidenceStatus.PENDING).count();
        long pendingFb = performanceFeedbackRepository.findBySupplierIdOrderByFeedbackDateDesc(supplierId).stream()
                .filter(f -> f.getEvidenceStatus() == EvidenceStatus.PENDING).count();
        long total = pendingPolicy + pendingQuality + pendingDelivery + pendingClaims + pendingEvents + pendingAs + pendingFb;
        return new EvidencePendingSummaryResponse(
                supplierId, pendingPolicy, pendingQuality, pendingDelivery,
                pendingClaims, pendingEvents, pendingAs, pendingFb, total);
    }


    @Transactional(readOnly = true)
    public java.util.List<DeliveryMetricResponse> listDelivery(String supplierId, EvidenceStatus evidenceStatus) {
        supplierMasterService.require(supplierId);
        java.util.List<SupplierDeliveryMetric> rows;
        if (evidenceStatus != null) {
            rows = deliveryRepository.findByCustomerIdAndSupplierIdAndEvidenceStatusOrderByPeriodEndDesc(tenantAccessGuard.requireCustomerId(), supplierId, evidenceStatus);
        } else {
            rows = deliveryRepository.findByCustomerIdAndSupplierIdOrderByPeriodEndDesc(tenantAccessGuard.requireCustomerId(), supplierId);
        }
        return rows.stream().map(this::toDelivery).toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<QualityMetricResponse> listQuality(String supplierId, EvidenceStatus evidenceStatus) {
        supplierMasterService.require(supplierId);
        java.util.List<SupplierQualityMetric> rows;
        if (evidenceStatus != null) {
            rows = qualityRepository.findByCustomerIdAndSupplierIdAndEvidenceStatusOrderByPeriodEndDesc(tenantAccessGuard.requireCustomerId(), supplierId, evidenceStatus);
        } else {
            rows = qualityRepository.findByCustomerIdAndSupplierIdOrderByPeriodEndDesc(tenantAccessGuard.requireCustomerId(), supplierId);
        }
        return rows.stream().map(this::toQuality).toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<AfterSalesMetricResponse> listAfterSales(String supplierId, EvidenceStatus evidenceStatus) {
        supplierMasterService.require(supplierId);
        java.util.List<SupplierAfterSalesMetric> rows;
        if (evidenceStatus != null) {
            rows = afterSalesRepository.findByCustomerIdAndSupplierIdAndEvidenceStatusOrderByPeriodEndDesc(tenantAccessGuard.requireCustomerId(), supplierId, evidenceStatus);
        } else {
            rows = afterSalesRepository.findByCustomerIdAndSupplierIdOrderByPeriodEndDesc(tenantAccessGuard.requireCustomerId(), supplierId);
        }
        return rows.stream().map(this::toAfterSales).toList();
    }

    public AfterSalesSummaryResponse afterSalesSummary(String supplierId) {
        supplierMasterService.require(supplierId);
        var metrics = afterSalesRepository.findByCustomerIdAndSupplierIdOrderByPeriodEndDesc(tenantAccessGuard.requireCustomerId(), supplierId);
        long verified = metrics.stream().filter(m -> m.getEvidenceStatus() == EvidenceStatus.VERIFIED).count();
        long pending = metrics.stream().filter(m -> m.getEvidenceStatus() == EvidenceStatus.PENDING).count();
        java.math.BigDecimal latestSla = metrics.isEmpty() ? null : metrics.get(0).getServiceSlaMetPercent();
        Integer latestUnresolved = metrics.isEmpty() ? null : metrics.get(0).getUnresolvedComplaints();
        return new AfterSalesSummaryResponse(
                supplierId, metrics.size(), verified, pending, latestSla, latestUnresolved,
                metrics.stream().map(this::toAfterSales).toList());
    }

    public GuaranteeEventSummaryResponse guaranteeEventSummary(String supplierId) {
        supplierMasterService.require(supplierId);
        var events = guaranteeEventRepository.findByCustomerIdAndSupplierIdOrderByEventDateDesc(tenantAccessGuard.requireCustomerId(), supplierId);
        long unresolved = events.stream().filter(e -> !e.isResolvedFlag()).count();
        long verified = events.stream().filter(e -> e.getEvidenceStatus() == EvidenceStatus.VERIFIED).count();
        java.math.BigDecimal totalPenalty = events.stream()
                .map(SupplierGuaranteeEvent::getPenaltyAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        String currency = events.stream().map(SupplierGuaranteeEvent::getCurrencyCode)
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);
        return new GuaranteeEventSummaryResponse(
                supplierId, events.size(), unresolved, verified, totalPenalty, currency,
                events.stream().map(this::toGuaranteeEvent).toList());
    }

    public Supplier360Response supplier360(String supplierId) {
        supplierMasterService.require(supplierId);
        var capabilities = capabilityRepository.findBySupplierIdAndStatus(supplierId, CapabilityStatus.ACTIVE)
                .stream()
                .map(c -> new CapabilityResponse(
                        c.getCapabilityId(), c.getSupplierId(), c.getCapabilityType(), c.getCapabilityCode(),
                        c.getCapabilityName(), c.getProcessCode(), c.getCommodityCode(), c.getPlantCode(),
                        c.getMaxAnnualCapacity(), c.getCapacityUom(), c.isCertified(),
                        c.getValidFrom(), c.getValidTo(), c.getStatus(), c.getNotes()))
                .toList();
        return new Supplier360Response(
                supplierId,
                qualityRepository.findByCustomerIdAndSupplierIdOrderByPeriodEndDesc(tenantAccessGuard.requireCustomerId(), supplierId).stream().map(this::toQuality).toList(),
                deliveryRepository.findByCustomerIdAndSupplierIdOrderByPeriodEndDesc(tenantAccessGuard.requireCustomerId(), supplierId).stream().map(this::toDelivery).toList(),
                warrantyRepository.findByCustomerIdAndSupplierIdOrderByCreatedAtDesc(tenantAccessGuard.requireCustomerId(), supplierId).stream().map(this::toWarranty).toList(),
                warrantyClaimRepository.findByCustomerIdAndSupplierIdOrderByClaimDateDesc(tenantAccessGuard.requireCustomerId(), supplierId).stream().map(this::toWarrantyClaim).toList(),
                guaranteeRepository.findByCustomerIdAndSupplierIdOrderByCreatedAtDesc(tenantAccessGuard.requireCustomerId(), supplierId).stream().map(this::toGuarantee).toList(),
                guaranteeEventRepository.findByCustomerIdAndSupplierIdOrderByEventDateDesc(tenantAccessGuard.requireCustomerId(), supplierId).stream().map(this::toGuaranteeEvent).toList(),
                afterSalesRepository.findByCustomerIdAndSupplierIdOrderByPeriodEndDesc(tenantAccessGuard.requireCustomerId(), supplierId).stream().map(this::toAfterSales).toList(),
                performanceFeedbackRepository.findBySupplierIdOrderByFeedbackDateDesc(supplierId).stream()
                        .map(f -> new PerformanceFeedbackResponse(
                                f.getFeedbackId(), f.getSupplierId(), f.getFeedbackDate(), f.getFeedbackType(),
                                f.getMetricCode(), f.getMetricValue(), f.getUnit(), f.getEvidenceStatus(),
                                f.isAppliedToHistory(), f.getHistoryMetricId(), f.getHistoryMetricKind(),
                                f.getNarrative()))
                        .toList(),
                capabilities,
                policyRepository.findByCustomerIdAndSupplierIdOrderByCreatedAtDesc(tenantAccessGuard.requireCustomerId(), supplierId).stream().map(this::toPolicy).toList());
    }

    private void applyVerify(SupplierQualityMetric m, VerifyEvidenceRequest req) {
        evidenceStatusTransitionEngine.assertTransitionAllowed(m.getEvidenceStatus(), req.evidenceStatus());
        m.setEvidenceStatus(req.evidenceStatus());
        if (req.evidenceRef() != null) m.setEvidenceRef(req.evidenceRef());
        m.setVerifiedBy(currentUserId());
        m.setVerifiedAt(Instant.now());
    }

    private void applyVerify(SupplierDeliveryMetric m, VerifyEvidenceRequest req) {
        evidenceStatusTransitionEngine.assertTransitionAllowed(m.getEvidenceStatus(), req.evidenceStatus());
        m.setEvidenceStatus(req.evidenceStatus());
        if (req.evidenceRef() != null) m.setEvidenceRef(req.evidenceRef());
        m.setVerifiedBy(currentUserId());
        m.setVerifiedAt(Instant.now());
    }

    private void applyVerify(SupplierWarrantyTerm m, VerifyEvidenceRequest req) {
        evidenceStatusTransitionEngine.assertTransitionAllowed(m.getEvidenceStatus(), req.evidenceStatus());
        m.setEvidenceStatus(req.evidenceStatus());
        if (req.evidenceRef() != null) m.setEvidenceRef(req.evidenceRef());
        m.setVerifiedBy(currentUserId());
        m.setVerifiedAt(Instant.now());
    }

    private void applyVerify(SupplierGuaranteeTerm m, VerifyEvidenceRequest req) {
        evidenceStatusTransitionEngine.assertTransitionAllowed(m.getEvidenceStatus(), req.evidenceStatus());
        m.setEvidenceStatus(req.evidenceStatus());
        if (req.evidenceRef() != null) m.setEvidenceRef(req.evidenceRef());
        m.setVerifiedBy(currentUserId());
        m.setVerifiedAt(Instant.now());
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.automotive.costing.platform.user.entity.User u) {
            return u.getUserId();
        }
        return null;
    }

    private QualityMetricResponse toQuality(SupplierQualityMetric m) {
        return new QualityMetricResponse(m.getMetricId(), m.getSupplierId(), m.getMetricType(),
                m.getPeriodStart(), m.getPeriodEnd(), m.getMetricValue(), m.getUnit(), m.getSampleSize(),
                m.getEvidenceStatus(), m.getEvidenceRef(), m.getVerifiedBy(), m.getVerifiedAt(), m.getNotes());
    }

    private DeliveryMetricResponse toDelivery(SupplierDeliveryMetric m) {
        return new DeliveryMetricResponse(m.getMetricId(), m.getSupplierId(), m.getPeriodStart(), m.getPeriodEnd(),
                m.getOtdPercent(), m.getOtifPercent(), m.getLateDeliveryCount(), m.getAvgDelayDays(),
                m.getEvidenceStatus(), m.getEvidenceRef(), m.getVerifiedBy(), m.getVerifiedAt(), m.getNotes());
    }

    private WarrantyResponse toWarranty(SupplierWarrantyTerm w) {
        return new WarrantyResponse(w.getWarrantyId(), w.getSupplierId(), w.getDurationMonths(),
                w.getCoverageSummary(), w.getExclusions(), w.getClaimSlaDays(), w.getRemedy(),
                w.getEffectiveFrom(), w.getEffectiveTo(), w.getClaimRate(), w.getHistoricalCost(),
                w.getCurrencyCode(), w.getEvidenceStatus(), w.getEvidenceRef(), w.getVerifiedBy(),
                w.getVerifiedAt(), w.getNotes());
    }

    private GuaranteeResponse toGuarantee(SupplierGuaranteeTerm g) {
        return new GuaranteeResponse(g.getGuaranteeId(), g.getSupplierId(), g.getGuaranteeType(),
                g.getDescription(), g.getPeriodMonths(), g.getConditionsText(), g.getFailureConsequence(),
                g.getEffectiveFrom(), g.getEffectiveTo(), g.getEvidenceStatus(), g.getEvidenceRef(),
                g.getVerifiedBy(), g.getVerifiedAt(), g.getNotes());
    }


    private WarrantyClaimResponse toWarrantyClaim(SupplierWarrantyClaim c) {
        return new WarrantyClaimResponse(
                c.getClaimId(), c.getSupplierId(), c.getWarrantyId(), c.getClaimDate(),
                c.getClaimNumber(), c.getPartNumber(), c.getClaimCost(), c.getCurrencyCode(),
                c.getClaimStatus(), c.getAcceptedFlag(), c.getResolutionDays(), c.getResolutionSummary(),
                c.getEvidenceStatus(), c.getEvidenceRef(), c.getVerifiedBy(), c.getVerifiedAt(), c.getNotes());
    }

    private GuaranteeEventResponse toGuaranteeEvent(SupplierGuaranteeEvent e) {
        return new GuaranteeEventResponse(
                e.getEventId(), e.getSupplierId(), e.getGuaranteeId(), e.getEventDate(),
                e.getEventType(), e.getDescription(), e.getConsequenceApplied(),
                e.getPenaltyAmount(), e.getCurrencyCode(), e.isResolvedFlag(),
                e.getResolutionSummary(), e.getEvidenceStatus(), e.getEvidenceRef(),
                e.getVerifiedBy(), e.getVerifiedAt(), e.getNotes());
    }

    private AfterSalesMetricResponse toAfterSales(SupplierAfterSalesMetric m) {
        return new AfterSalesMetricResponse(
                m.getMetricId(), m.getSupplierId(), m.getPeriodStart(), m.getPeriodEnd(),
                m.getResponseTimeHours(), m.getResolutionTimeHours(),
                m.getEmergencySupportFlag(), m.getFieldSupportFlag(),
                m.getSparePartsAvailable(), m.getLocalSupportFlag(),
                m.getUnresolvedComplaints(), m.getAvgClosureDays(), m.getServiceSlaMetPercent(),
                m.getEvidenceStatus(), m.getEvidenceRef(), m.getVerifiedBy(), m.getVerifiedAt(), m.getNotes());
    }

    private PolicyEvidenceResponse toPolicy(SupplierPolicyEvidence p) {
        return new PolicyEvidenceResponse(
                p.getPolicyId(), p.getSupplierId(), p.getPolicyType(), p.getPolicyTitle(),
                p.getDescription(), p.getPolicyVersion(), p.getEffectiveFrom(), p.getEffectiveTo(),
                p.getExpiryDate(), p.isDeclarationOnly(), p.getEvidenceStatus(), p.getEvidenceRef(),
                p.getVerifiedBy(), p.getVerifiedAt(), p.getWaiverReason(), p.getNotes());
    }
}
