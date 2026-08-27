package com.automotive.costing.notification.service;

import com.automotive.costing.notification.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Wave F — polls email delivery queue when email is enabled.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailDeliveryScheduler {

    private final EmailDeliveryQueueService queueService;
    private final NotificationProperties properties;

    @Scheduled(fixedDelayString = "${app.notification.queue-poll-ms:30000}")
    public void poll() {
        if (!properties.isEmailEnabled()) {
            return;
        }
        int n = queueService.processDue(20);
        if (n > 0) {
            log.debug("Processed {} email queue item(s)", n);
        }
    }
}
