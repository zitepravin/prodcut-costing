package com.automotive.costing.vendorselection.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.vendorselection.entity.EvidenceExpiryJobStatus;
import com.automotive.costing.vendorselection.entity.EvidenceExpiryRun;
import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import com.automotive.costing.vendorselection.entity.SupplierGuaranteeTerm;
import com.automotive.costing.vendorselection.entity.SupplierPolicyEvidence;
import com.automotive.costing.vendorselection.entity.SupplierWarrantyTerm;
import com.automotive.costing.vendorselection.repository.EvidenceExpiryJobStatusRepository;
import com.automotive.costing.vendorselection.repository.EvidenceExpiryRunRepository;
import com.automotive.costing.vendorselection.repository.SupplierGuaranteeTermRepository;
import com.automotive.costing.vendorselection.repository.SupplierPolicyEvidenceRepository;
import com.automotive.costing.vendorselection.repository.SupplierWarrantyTermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Marks VERIFIED (or PENDING) evidence as EXPIRED when past expiry/effective-to date.
 * Does not invent status transitions other than → EXPIRED.
 * Supports MANUAL and SCHEDULED trigger sources (V52).
 */
@Service
@RequiredArgsConstructor
public class EvidenceExpiryService {

    public static final String JOB_NAME = "EVIDENCE_EXPIRY_DAILY";
    public static final String TRIGGER_MANUAL = "MANUAL";
    public static final String TRIGGER_SCHEDULED = "SCHEDULED";

    private final SupplierPolicyEvidenceRepository policyRepository;
    private final SupplierWarrantyTermRepository warrantyRepository;
    private final SupplierGuaranteeTermRepository guaranteeRepository;
    private final EvidenceExpiryRunRepository runRepository;
    private final EvidenceExpiryJobStatusRepository jobStatusRepository;
    private final AuditService auditService;

    public record ExpiryRunResponse(
            String runId,
            LocalDate asOfDate,
            int policiesExpired,
            int warrantiesExpired,
            int guaranteesExpired,
            Instant ranAt,
            String triggerSource
    ) {}

    public record JobStatusResponse(
            String jobName,
            boolean enabled,
            Instant lastScheduledAt,
            String lastRunId,
            Boolean lastSuccess,
            String lastError
    ) {}

    @Transactional
    public ExpiryRunResponse runExpiryPass(LocalDate asOfDate) {
        return runExpiryPass(asOfDate, TRIGGER_MANUAL);
    }

    @Transactional
    public ExpiryRunResponse runExpiryPass(LocalDate asOfDate, String triggerSource) {
        LocalDate asOf = asOfDate != null ? asOfDate : LocalDate.now();
        String source = triggerSource == null || triggerSource.isBlank() ? TRIGGER_MANUAL : triggerSource.trim().toUpperCase();
        int policies = expirePolicies(asOf);
        int warranties = expireWarranties(asOf);
        int guarantees = expireGuarantees(asOf);

        EvidenceExpiryRun run = new EvidenceExpiryRun();
        run.setRunId(UUID.randomUUID().toString());
        run.setRanAt(Instant.now());
        run.setRanBy(currentUserId());
        run.setAsOfDate(asOf);
        run.setPoliciesExpired(policies);
        run.setWarrantiesExpired(warranties);
        run.setGuaranteesExpired(guarantees);
        run.setTriggerSource(source);
        run.setNotes("Expired policies=" + policies + ", warranties=" + warranties + ", guarantees=" + guarantees);
        runRepository.save(run);
        auditService.recordCreate("EvidenceExpiryRun", run.getRunId(),
                source + " asOf=" + asOf + " p=" + policies + " w=" + warranties + " g=" + guarantees);

        return new ExpiryRunResponse(
                run.getRunId(), run.getAsOfDate(), policies, warranties, guarantees, run.getRanAt(), source);
    }

    /**
     * Invoked by scheduler. Honors DB enabled_flag and records job status.
     */
    @Transactional
    public ExpiryRunResponse runScheduledPass() {
        EvidenceExpiryJobStatus status = jobStatusRepository.findByJobName(JOB_NAME)
                .orElseGet(this::bootstrapJobStatus);
        status.setLastScheduledAt(Instant.now());
        if (!status.isEnabledFlag()) {
            status.setLastSuccess(true);
            status.setLastError("Skipped — job disabled");
            status.setUpdatedAt(Instant.now());
            jobStatusRepository.save(status);
            return null;
        }
        try {
            ExpiryRunResponse result = runExpiryPass(LocalDate.now(), TRIGGER_SCHEDULED);
            status.setLastRunId(result.runId());
            status.setLastSuccess(true);
            status.setLastError(null);
            status.setUpdatedAt(Instant.now());
            jobStatusRepository.save(status);
            return result;
        } catch (RuntimeException ex) {
            status.setLastSuccess(false);
            status.setLastError(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
            status.setUpdatedAt(Instant.now());
            jobStatusRepository.save(status);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<ExpiryRunResponse> recentRuns() {
        return runRepository.findTop20ByOrderByRanAtDesc().stream()
                .map(r -> new ExpiryRunResponse(
                        r.getRunId(), r.getAsOfDate(), r.getPoliciesExpired(), r.getWarrantiesExpired(),
                        r.getGuaranteesExpired(), r.getRanAt(),
                        r.getTriggerSource() != null ? r.getTriggerSource() : TRIGGER_MANUAL))
                .toList();
    }

    @Transactional(readOnly = true)
    public JobStatusResponse jobStatus() {
        EvidenceExpiryJobStatus status = jobStatusRepository.findByJobName(JOB_NAME)
                .orElseGet(this::bootstrapJobStatus);
        return new JobStatusResponse(
                status.getJobName(),
                status.isEnabledFlag(),
                status.getLastScheduledAt(),
                status.getLastRunId(),
                status.getLastSuccess(),
                status.getLastError());
    }

    @Transactional
    public JobStatusResponse setJobEnabled(boolean enabled) {
        EvidenceExpiryJobStatus status = jobStatusRepository.findByJobName(JOB_NAME)
                .orElseGet(this::bootstrapJobStatus);
        status.setEnabledFlag(enabled);
        status.setUpdatedAt(Instant.now());
        jobStatusRepository.save(status);
        auditService.recordUpdate("EvidenceExpiryJobStatus", status.getStatusId(), null,
                enabled ? "ENABLED" : "DISABLED");
        return jobStatus();
    }

    private EvidenceExpiryJobStatus bootstrapJobStatus() {
        EvidenceExpiryJobStatus s = new EvidenceExpiryJobStatus();
        s.setStatusId(UUID.randomUUID().toString());
        s.setJobName(JOB_NAME);
        s.setEnabledFlag(true);
        s.setUpdatedAt(Instant.now());
        return jobStatusRepository.save(s);
    }

    private int expirePolicies(LocalDate asOf) {
        int count = 0;
        for (SupplierPolicyEvidence p : policyRepository.findByEvidenceStatusIn(java.util.List.of(EvidenceStatus.VERIFIED, EvidenceStatus.PENDING))) {
            if (p.getExpiryDate() == null) {
                continue;
            }
            if (p.getEvidenceStatus() == EvidenceStatus.EXPIRED) {
                continue;
            }
            if (p.getEvidenceStatus() == EvidenceStatus.VERIFIED || p.getEvidenceStatus() == EvidenceStatus.PENDING) {
                if (asOf.isAfter(p.getExpiryDate())) {
                    p.setEvidenceStatus(EvidenceStatus.EXPIRED);
                    policyRepository.save(p);
                    auditService.recordUpdate("SupplierPolicyEvidence", p.getPolicyId(), null, "EXPIRED");
                    count++;
                }
            }
        }
        return count;
    }

    private int expireWarranties(LocalDate asOf) {
        int count = 0;
        for (SupplierWarrantyTerm w : warrantyRepository.findByEvidenceStatusIn(java.util.List.of(EvidenceStatus.VERIFIED, EvidenceStatus.PENDING))) {
            if (w.getEffectiveTo() == null || w.getEvidenceStatus() == EvidenceStatus.EXPIRED) {
                continue;
            }
            if ((w.getEvidenceStatus() == EvidenceStatus.VERIFIED || w.getEvidenceStatus() == EvidenceStatus.PENDING)
                    && asOf.isAfter(w.getEffectiveTo())) {
                w.setEvidenceStatus(EvidenceStatus.EXPIRED);
                warrantyRepository.save(w);
                auditService.recordUpdate("SupplierWarrantyTerm", w.getWarrantyId(), null, "EXPIRED");
                count++;
            }
        }
        return count;
    }

    private int expireGuarantees(LocalDate asOf) {
        int count = 0;
        for (SupplierGuaranteeTerm g : guaranteeRepository.findByEvidenceStatusIn(java.util.List.of(EvidenceStatus.VERIFIED, EvidenceStatus.PENDING))) {
            if (g.getEffectiveTo() == null || g.getEvidenceStatus() == EvidenceStatus.EXPIRED) {
                continue;
            }
            if ((g.getEvidenceStatus() == EvidenceStatus.VERIFIED || g.getEvidenceStatus() == EvidenceStatus.PENDING)
                    && asOf.isAfter(g.getEffectiveTo())) {
                g.setEvidenceStatus(EvidenceStatus.EXPIRED);
                guaranteeRepository.save(g);
                auditService.recordUpdate("SupplierGuaranteeTerm", g.getGuaranteeId(), null, "EXPIRED");
                count++;
            }
        }
        return count;
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return auth.getName();
    }
}
