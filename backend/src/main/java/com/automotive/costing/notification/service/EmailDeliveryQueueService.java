package com.automotive.costing.notification.service;

import com.automotive.costing.notification.config.NotificationProperties;
import com.automotive.costing.notification.entity.AppNotification;
import com.automotive.costing.notification.entity.DeliveryStatus;
import com.automotive.costing.notification.entity.NotificationDelivery;
import com.automotive.costing.notification.entity.NotificationTemplate;
import com.automotive.costing.notification.repository.NotificationDeliveryRepository;
import com.automotive.costing.notification.repository.NotificationTemplateRepository;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.repository.UserRepository;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Wave F — queue-based email delivery with idempotency and retry.
 * Does not place SMTP calls inside controllers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailDeliveryQueueService {

    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationPreferenceService preferenceService;
    private final EmailTemplateRenderer renderer;
    private final UserRepository userRepository;
    private final NotificationProperties properties;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    /**
     * Enqueue email for an in-app notification. Idempotent per notification id.
     */
    @Transactional
    public Optional<NotificationDelivery> enqueueFromNotification(AppNotification notification, String moduleCode) {
        if (notification == null || notification.getNotificationId() == null) {
            return Optional.empty();
        }
        if (!properties.isEmailEnabled()) {
            log.debug("Email disabled; skip queue for {}", notification.getNotificationId());
            return Optional.empty();
        }

        String eventType = notification.getNotificationType() != null
                ? notification.getNotificationType().name() : "SYSTEM";
        String mod = moduleCode != null ? moduleCode : inferModule(eventType);

        if (!preferenceService.isEmailEnabled(notification.getRecipientUserId(), mod, eventType)) {
            log.debug("User opted out of email for {} / {}", mod, eventType);
            return Optional.empty();
        }

        String idempotencyKey = "notif:" + notification.getNotificationId();
        Optional<NotificationDelivery> existing = deliveryRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.debug("Idempotent skip delivery for key {}", idempotencyKey);
            return existing;
        }

        User user = userRepository.findById(notification.getRecipientUserId()).orElse(null);
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("No email for user {}; skip queue", notification.getRecipientUserId());
            return Optional.empty();
        }

        Map<String, String> vars = new HashMap<>();
        vars.put("userName", user.getUsername());
        vars.put("documentNumber", notification.getEntityId() != null ? notification.getEntityId() : "");
        vars.put("status", eventType);
        vars.put("reason", "");

        String subject = notification.getTitle();
        String body = notification.getBody();
        String templateCode = null;
        Optional<NotificationTemplate> tmpl = templateRepository
                .findFirstByEventTypeAndLanguageCodeAndActiveFlagTrue(eventType, "en");
        if (tmpl.isPresent()) {
            NotificationTemplate t = tmpl.get();
            templateCode = t.getTemplateCode();
            subject = renderer.render(t.getSubjectTemplate(), vars);
            body = renderer.render(t.getBodyTemplate(), vars);
            if (body.isBlank()) {
                body = notification.getBody();
            }
        } else {
            body = "Hello " + user.getUsername() + ",\n\n" + notification.getBody()
                    + "\n\nSign in to Notifications to review.\n";
        }

        NotificationDelivery d = new NotificationDelivery();
        d.setDeliveryId(UUID.randomUUID().toString());
        d.setNotificationId(notification.getNotificationId());
        d.setIdempotencyKey(idempotencyKey);
        d.setRecipientUserId(notification.getRecipientUserId());
        d.setRecipientEmail(user.getEmail());
        d.setTemplateCode(templateCode);
        d.setEventType(eventType);
        d.setSubjectRendered(truncate(subject, 500));
        d.setBodyRendered(truncate(body, 4000));
        d.setStatus(DeliveryStatus.QUEUED);
        d.setAttemptCount(0);
        d.setMaxAttempts(3);
        d.setCorrelationId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        d.setScheduledAt(Instant.now());
        d.setCreatedAt(Instant.now());
        return Optional.of(deliveryRepository.save(d));
    }

    /** Process due queue items (QUEUED / RETRYING). */
    @Transactional
    public int processDue(int limit) {
        if (!properties.isEmailEnabled()) {
            return 0;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("JavaMailSender missing; cannot process email queue");
            return 0;
        }
        List<NotificationDelivery> due = deliveryRepository.lockDueBatch(Instant.now(), limit);
        int processed = 0;
        for (NotificationDelivery d : due) {
            processOne(mailSender, d);
            processed++;
        }
        return processed;
    }

    private void processOne(JavaMailSender mailSender, NotificationDelivery d) {
        d.setStatus(DeliveryStatus.PROCESSING);
        d.setAttemptCount(d.getAttemptCount() + 1);
        d.setUpdatedAt(Instant.now());
        deliveryRepository.save(d);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(new InternetAddress(properties.getMailFrom(), properties.getMailFromName()));
            helper.setTo(d.getRecipientEmail());
            helper.setSubject(d.getSubjectRendered() != null ? d.getSubjectRendered() : "Notification");
            helper.setText(d.getBodyRendered() != null ? d.getBodyRendered() : "", false);
            mailSender.send(message);
            d.setStatus(DeliveryStatus.SENT);
            d.setProcessedAt(Instant.now());
            d.setLastError(null);
            d.setUpdatedAt(Instant.now());
            deliveryRepository.save(d);
            log.info("Email SENT delivery={} corr={} to={}", d.getDeliveryId(), d.getCorrelationId(), d.getRecipientEmail());
        } catch (Exception ex) {
            String err = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            if (err.length() > 1000) {
                err = err.substring(0, 1000);
            }
            d.setLastError(err);
            d.setUpdatedAt(Instant.now());
            if (d.getAttemptCount() >= d.getMaxAttempts()) {
                d.setStatus(DeliveryStatus.FAILED);
                d.setProcessedAt(Instant.now());
            } else {
                d.setStatus(DeliveryStatus.RETRYING);
                d.setScheduledAt(Instant.now().plusSeconds(60L * d.getAttemptCount()));
            }
            deliveryRepository.save(d);
            log.error("Email delivery {} failed attempt {}: {}", d.getDeliveryId(), d.getAttemptCount(), err);
        }
    }

    private static String inferModule(String eventType) {
        if (eventType == null) {
            return "PRODUCT_COSTING";
        }
        if (eventType.startsWith("PURCHASE_")) {
            return "PURCHASE";
        }
        if (eventType.startsWith("VENDOR_")) {
            return "VENDOR_SELECTION";
        }
        return "PRODUCT_COSTING";
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
