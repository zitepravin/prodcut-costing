package com.automotive.costing.notification.listener;

import com.automotive.costing.common.event.VendorRecommendationDecisionEventMessage;
import com.automotive.costing.notification.entity.NotificationSeverity;
import com.automotive.costing.notification.entity.NotificationType;
import com.automotive.costing.notification.service.NotificationService;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * In-app notifications for human recommendation decisions.
 * Does not approve suppliers or award business.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VendorRecommendationDecisionNotificationListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDecision(VendorRecommendationDecisionEventMessage event) {
        NotificationType type = mapType(event.eventType());
        NotificationSeverity severity = switch (type) {
            case VENDOR_RECOMMENDATION_APPROVED -> NotificationSeverity.INFO;
            case VENDOR_RECOMMENDATION_REJECTED -> NotificationSeverity.WARNING;
            case VENDOR_RECOMMENDATION_OVERRIDDEN -> NotificationSeverity.WARNING;
            default -> NotificationSeverity.INFO;
        };
        String title = "Vendor recommendation " + event.newStatus();
        String body = "Recommendation " + event.recommendationId()
                + " transitioned " + event.previousStatus() + " → " + event.newStatus()
                + (event.recommendedSupplierId() != null ? " (supplier " + event.recommendedSupplierId() + ")" : "")
                + ". Human decision only — system does not auto-approve.";
        for (String role : List.of("SOURCING_LEAD", "COSTING_MANAGER", "PURCHASING_MANAGER")) {
            notifyRole(role, type, title, body, "VendorRecommendation", event.recommendationId(), severity);
        }
        if (event.occurredBy() != null && !event.occurredBy().isBlank()) {
            // Actor already knows; still useful for audit inbox consistency is optional — skip self-spam
        }
        log.info("Vendor recommendation decision notification emitted for {} ({})",
                event.recommendationId(), event.eventType());
    }

    private NotificationType mapType(String eventType) {
        if (eventType == null) {
            return NotificationType.SYSTEM;
        }
        return switch (eventType) {
            case "RECOMMENDATION_SUBMIT" -> NotificationType.VENDOR_RECOMMENDATION_SUBMITTED;
            case "RECOMMENDATION_APPROVE" -> NotificationType.VENDOR_RECOMMENDATION_APPROVED;
            case "RECOMMENDATION_REJECT" -> NotificationType.VENDOR_RECOMMENDATION_REJECTED;
            case "RECOMMENDATION_OVERRIDE" -> NotificationType.VENDOR_RECOMMENDATION_OVERRIDDEN;
            default -> NotificationType.SYSTEM;
        };
    }

    private void notifyRole(
            String roleCode,
            NotificationType type,
            String title,
            String body,
            String entityType,
            String entityId,
            NotificationSeverity severity) {
        List<User> users = userRepository.findActiveByRoleCode(roleCode);
        Set<String> seen = new HashSet<>();
        for (User u : users) {
            if (seen.add(u.getUserId())) {
                notificationService.publish(u.getUserId(), type, title, body, entityType, entityId, severity);
            }
        }
    }
}
