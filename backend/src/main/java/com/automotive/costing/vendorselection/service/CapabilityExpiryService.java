package com.automotive.costing.vendorselection.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.vendorselection.entity.CapabilityExpiryJobStatus;
import com.automotive.costing.vendorselection.entity.CapabilityExpiryRun;
import com.automotive.costing.vendorselection.entity.CapabilityStatus;
import com.automotive.costing.vendorselection.entity.SupplierCapability;
import com.automotive.costing.vendorselection.repository.CapabilityExpiryJobStatusRepository;
import com.automotive.costing.vendorselection.repository.CapabilityExpiryRunRepository;
import com.automotive.costing.vendorselection.repository.SupplierCapabilityRepository;
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
 * Marks ACTIVE capabilities as EXPIRED when past valid_to.
 * Does not invent other status transitions. Eligibility already ignores expired-by-date ACTIVE rows;
 * this persists status for operational clarity.
 */
@Service
@RequiredArgsConstructor
public class CapabilityExpiryService {

    public static final String JOB_NAME = "CAPABILITY_EXPIRY_DAILY";
    public static final String TRIGGER_MANUAL = "MANUAL";
    public static final String TRIGGER_SCHEDULED = "SCHEDULED";

    private final SupplierCapabilityRepository capabilityRepository;
    private final CapabilityExpiryRunRepository runRepository;
    private final CapabilityExpiryJobStatusRepository jobStatusRepository;
    private final AuditService auditService;

    public record ExpiryRunResponse(
            String runId,
            LocalDate asOfDate,
            int capabilitiesExpired,
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
        int expired = 0;
        for (SupplierCapability c : capabilityRepository.findByStatus(CapabilityStatus.ACTIVE)) {
            if (c.getValidTo() != null && asOf.isAfter(c.getValidTo())) {
                c.setStatus(CapabilityStatus.EXPIRED);
                capabilityRepository.save(c);
                auditService.recordUpdate("SupplierCapability", c.getCapabilityId(), "ACTIVE", "EXPIRED");
                expired++;
            }
        }
        CapabilityExpiryRun run = new CapabilityExpiryRun();
        run.setRunId(UUID.randomUUID().toString());
        run.setRanAt(Instant.now());
        run.setRanBy(currentUserId());
        run.setAsOfDate(asOf);
        run.setCapabilitiesExpired(expired);
        run.setTriggerSource(source);
        run.setNotes("Expired capabilities=" + expired);
        runRepository.save(run);
        auditService.recordCreate("CapabilityExpiryRun", run.getRunId(), source + " n=" + expired);
        return new ExpiryRunResponse(run.getRunId(), asOf, expired, run.getRanAt(), source);
    }

    @Transactional
    public ExpiryRunResponse runScheduledPass() {
        CapabilityExpiryJobStatus status = jobStatusRepository.findByJobName(JOB_NAME)
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
                        r.getRunId(), r.getAsOfDate(), r.getCapabilitiesExpired(), r.getRanAt(),
                        r.getTriggerSource()))
                .toList();
    }

    @Transactional(readOnly = true)
    public JobStatusResponse jobStatus() {
        CapabilityExpiryJobStatus status = jobStatusRepository.findByJobName(JOB_NAME)
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
        CapabilityExpiryJobStatus status = jobStatusRepository.findByJobName(JOB_NAME)
                .orElseGet(this::bootstrapJobStatus);
        status.setEnabledFlag(enabled);
        status.setUpdatedAt(Instant.now());
        jobStatusRepository.save(status);
        auditService.recordUpdate("CapabilityExpiryJobStatus", status.getStatusId(), null,
                enabled ? "ENABLED" : "DISABLED");
        return jobStatus();
    }

    private CapabilityExpiryJobStatus bootstrapJobStatus() {
        CapabilityExpiryJobStatus s = new CapabilityExpiryJobStatus();
        s.setStatusId(UUID.randomUUID().toString());
        s.setJobName(JOB_NAME);
        s.setEnabledFlag(true);
        s.setUpdatedAt(Instant.now());
        return jobStatusRepository.save(s);
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
}
