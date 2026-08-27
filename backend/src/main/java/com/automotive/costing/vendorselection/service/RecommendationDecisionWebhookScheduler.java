package com.automotive.costing.vendorselection.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "vendor-selection.decision-webhook.enabled", havingValue = "true")
public class RecommendationDecisionWebhookScheduler {

    private final RecommendationDecisionWebhookService webhookService;

    @Scheduled(fixedDelayString = "${vendor-selection.decision-webhook.poll-interval-ms:60000}")
    public void pollAndDispatch() {
        var summary = webhookService.dispatchUnpublished();
        if (summary.attempted() > 0) {
            log.info("Decision webhook dispatch attempted={} succeeded={} failed={}",
                    summary.attempted(), summary.succeeded(), summary.failed());
        }
    }
}
