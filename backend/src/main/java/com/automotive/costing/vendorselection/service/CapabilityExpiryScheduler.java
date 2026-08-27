package com.automotive.costing.vendorselection.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily capability expiry sweep (ACTIVE → EXPIRED when past valid_to).
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "vendor-selection.capability-expiry.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class CapabilityExpiryScheduler {

    private final CapabilityExpiryService capabilityExpiryService;

    @Scheduled(cron = "${vendor-selection.capability-expiry.cron:0 45 2 * * *}")
    public void scheduledExpiryPass() {
        try {
            var result = capabilityExpiryService.runScheduledPass();
            if (result == null) {
                log.info("Capability expiry scheduled pass skipped (disabled)");
                return;
            }
            log.info("Capability expiry runId={} expired={}", result.runId(), result.capabilitiesExpired());
        } catch (Exception e) {
            log.error("Capability expiry scheduled pass failed: {}", e.getMessage(), e);
        }
    }
}
