package com.automotive.costing.vendorselection.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily evidence expiry sweep. Disabled when vendor-selection.evidence-expiry.scheduler-enabled=false.
 * DB job status can also disable runs without redeploy.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "vendor-selection.evidence-expiry.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class EvidenceExpiryScheduler {

    private final EvidenceExpiryService evidenceExpiryService;

    @Value("${vendor-selection.evidence-expiry.cron:0 30 2 * * *}")
    private String cronExpression;

    @Scheduled(cron = "${vendor-selection.evidence-expiry.cron:0 30 2 * * *}")
    public void scheduledExpiryPass() {
        try {
            var result = evidenceExpiryService.runScheduledPass();
            if (result == null) {
                log.info("Evidence expiry scheduled pass skipped (disabled in job status)");
                return;
            }
            log.info("Evidence expiry scheduled pass runId={} policies={} warranties={} guarantees={}",
                    result.runId(), result.policiesExpired(), result.warrantiesExpired(), result.guaranteesExpired());
        } catch (Exception e) {
            log.error("Evidence expiry scheduled pass failed: {}", e.getMessage(), e);
        }
    }
}
