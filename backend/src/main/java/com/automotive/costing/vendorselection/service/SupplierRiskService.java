package com.automotive.costing.vendorselection.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.entity.*;
import com.automotive.costing.vendorselection.repository.SupplierRiskRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.vendorselection.engine.RiskStatusTransitionEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Supplier risk register — tracked separately from scorecard overall score.
 */
@Service
@RequiredArgsConstructor
public class SupplierRiskService {

    private final SupplierRiskRepository riskRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final SupplierMasterService supplierMasterService;
    private final AuditService auditService;
    private final RiskStatusTransitionEngine riskStatusTransitionEngine;

    public record CreateRiskRequest(
            String supplierId,
            RiskCategory riskCategory,
            String riskTitle,
            String description,
            RiskSeverity severity,
            RiskLikelihood likelihood,
            String mitigationPlan,
            String evidenceRef
    ) {}

    public record UpdateRiskStatusRequest(
            RiskStatus status,
            String mitigationPlan,
            RiskSeverity residualSeverity,
            String evidenceRef
    ) {}

    public record RiskResponse(
            String riskId,
            String supplierId,
            RiskCategory riskCategory,
            String riskTitle,
            String description,
            RiskSeverity severity,
            RiskLikelihood likelihood,
            RiskStatus status,
            String mitigationPlan,
            RiskSeverity residualSeverity,
            EvidenceStatus evidenceStatus,
            String evidenceRef,
            Instant identifiedAt,
            Instant closedAt
    ) {}

    public record RiskSummaryResponse(
            String supplierId,
            long openCount,
            long criticalOpenCount,
            long highOpenCount,
            List<RiskResponse> openRisks
    ) {}

    @Transactional
    public RiskResponse create(CreateRiskRequest request) {
        if (request.supplierId() == null || request.riskCategory() == null
                || request.riskTitle() == null || request.riskTitle().isBlank()
                || request.severity() == null) {
            throw new BusinessRuleViolationException(
                    "supplierId, riskCategory, riskTitle, severity are required", "VS-RISK-01");
        }
        supplierMasterService.require(request.supplierId());
        SupplierRisk r = new SupplierRisk();
        r.setRiskId(UUID.randomUUID().toString());
        r.setCustomerId(tenantAccessGuard.requireCustomerId());
        r.setSupplierId(request.supplierId());
        r.setRiskCategory(request.riskCategory());
        r.setRiskTitle(request.riskTitle().trim());
        r.setDescription(request.description());
        r.setSeverity(request.severity());
        r.setLikelihood(request.likelihood());
        r.setStatus(RiskStatus.OPEN);
        r.setMitigationPlan(request.mitigationPlan());
        r.setEvidenceStatus(EvidenceStatus.PENDING);
        r.setEvidenceRef(request.evidenceRef());
        r.setIdentifiedAt(Instant.now());
        r.setIdentifiedBy(currentUserId());
        riskRepository.save(r);
        auditService.recordCreate("SupplierRisk", r.getRiskId(),
                request.riskCategory().name() + "/" + request.severity().name());
        return toResponse(r);
    }

    @Transactional
    public RiskResponse updateStatus(String riskId, UpdateRiskStatusRequest request) {
        String customerId = tenantAccessGuard.requireCustomerId();
        SupplierRisk r = riskRepository.findByCustomerIdAndRiskId(customerId, riskId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupplierRisk", riskId));
        tenantAccessGuard.assertOwns(r.getCustomerId(), "SupplierRisk", riskId);
        if (request.status() == null) {
            throw new BusinessRuleViolationException("status is required", "VS-RISK-02");
        }
        RiskStatus previousStatus = r.getStatus();
        riskStatusTransitionEngine.assertTransitionAllowed(previousStatus, request.status());
        if (request.status() == RiskStatus.ACCEPTED && (request.mitigationPlan() == null
                || request.mitigationPlan().isBlank())) {
            throw new BusinessRuleViolationException(
                    "mitigationPlan or acceptance rationale required when accepting risk", "VS-RISK-03");
        }
        r.setStatus(request.status());
        if (request.mitigationPlan() != null) {
            r.setMitigationPlan(request.mitigationPlan());
        }
        if (request.residualSeverity() != null) {
            r.setResidualSeverity(request.residualSeverity());
        }
        if (request.evidenceRef() != null) {
            r.setEvidenceRef(request.evidenceRef());
        }
        if (request.status() == RiskStatus.CLOSED || request.status() == RiskStatus.ACCEPTED) {
            r.setClosedAt(Instant.now());
            r.setClosedBy(currentUserId());
        }
        riskRepository.save(r);
        auditService.recordTransition("SupplierRisk", riskId,
                previousStatus != null ? previousStatus.name() : null, request.status().name());
        return toResponse(r);
    }

    @Transactional(readOnly = true)
    public List<RiskResponse> listBySupplier(String supplierId) {
        return listBySupplier(supplierId, null, null);
    }

    @Transactional(readOnly = true)
    public List<RiskResponse> listBySupplier(String supplierId, RiskStatus status, RiskSeverity severity) {
        supplierMasterService.require(supplierId);
        List<SupplierRisk> rows;
        if (status != null && severity != null) {
            rows = riskRepository.findByCustomerIdAndSupplierIdAndStatusAndSeverityOrderByIdentifiedAtDesc(
                    tenantAccessGuard.requireCustomerId(), supplierId, status, severity);
        } else if (status != null) {
            rows = riskRepository.findByCustomerIdAndSupplierIdAndStatusOrderByIdentifiedAtDesc(
                    tenantAccessGuard.requireCustomerId(), supplierId, status);
        } else {
            rows = riskRepository.findByCustomerIdAndSupplierIdOrderByIdentifiedAtDesc(
                    tenantAccessGuard.requireCustomerId(), supplierId);
        }
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RiskSummaryResponse summary(String supplierId) {
        supplierMasterService.require(supplierId);
        List<SupplierRisk> open = riskRepository.findByCustomerIdAndSupplierIdAndStatusOrderByIdentifiedAtDesc(tenantAccessGuard.requireCustomerId(), 
                supplierId, RiskStatus.OPEN);
        open = new java.util.ArrayList<>(open);
        open.addAll(riskRepository.findByCustomerIdAndSupplierIdAndStatusOrderByIdentifiedAtDesc(tenantAccessGuard.requireCustomerId(), 
                supplierId, RiskStatus.MITIGATING));
        long critical = open.stream().filter(r -> r.getSeverity() == RiskSeverity.CRITICAL).count();
        long high = open.stream().filter(r -> r.getSeverity() == RiskSeverity.HIGH).count();
        return new RiskSummaryResponse(supplierId, open.size(), critical, high,
                open.stream().map(this::toResponse).toList());
    }

    private RiskResponse toResponse(SupplierRisk r) {
        return new RiskResponse(
                r.getRiskId(), r.getSupplierId(), r.getRiskCategory(), r.getRiskTitle(),
                r.getDescription(), r.getSeverity(), r.getLikelihood(), r.getStatus(),
                r.getMitigationPlan(), r.getResidualSeverity(), r.getEvidenceStatus(),
                r.getEvidenceRef(), r.getIdentifiedAt(), r.getClosedAt());
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.automotive.costing.platform.user.entity.User u) {
            return u.getUserId();
        }
        return null;
    }
}
