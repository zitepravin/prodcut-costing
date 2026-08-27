package com.automotive.costing.notification.listener;

import com.automotive.costing.common.event.SupplierQualificationLifecycleEventMessage;
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
 * In-app notifications for supplier qualification lifecycle transitions.
 * Does not approve suppliers or award business.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VendorQualificationLifecycleNotificationListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLifecycle(SupplierQualificationLifecycleEventMessage event) {
        NotificationType type = mapType(event.newStatus());
        NotificationSeverity severity = switch (type) {
            case VENDOR_QUALIFICATION_APPROVED -> NotificationSeverity.INFO;
            case VENDOR_QUALIFICATION_REJECTED, VENDOR_QUALIFICATION_BLOCKED -> NotificationSeverity.WARNING;
            default -> NotificationSeverity.INFO;
        };
        String title = "Supplier qualification " + event.newStatus();
        String body = "Supplier " + event.supplierId()
                + " lifecycle " + (event.previousStatus() != null ? event.previousStatus() : "null")
                + " → " + event.newStatus()
                + (event.recommendationId() != null ? " (recommendation " + event.recommendationId() + ")" : "")
                + ". Human-controlled — system does not auto-approve.";
        for (String role : List.of("SOURCING_LEAD", "COSTING_MANAGER", "PURCHASING_MANAGER", "QUALITY_ENGINEER")) {
            notifyRole(role, type, title, body, "SupplierQualification", event.qualificationId(), severity);
        }
        log.info("Qualification lifecycle notification for supplier {} ({})", event.supplierId(), event.newStatus());
    }

    private NotificationType mapType(String newStatus) {
        if (newStatus == null) {
            return NotificationType.VENDOR_QUALIFICATION_TRANSITION;
        }
        return switch (newStatus.toUpperCase()) {
            case "APPROVED" -> NotificationType.VENDOR_QUALIFICATION_APPROVED;
            case "REJECTED" -> NotificationType.VENDOR_QUALIFICATION_REJECTED;
            case "BLOCKED" -> NotificationType.VENDOR_QUALIFICATION_BLOCKED;
            default -> NotificationType.VENDOR_QUALIFICATION_TRANSITION;
        };
    }

    private void notifyRole(String roleCode, NotificationType type, String title, String body,
                            String entityType, String entityId, NotificationSeverity severity) {
        List<User> users = userRepository.findActiveByRoleCode(roleCode);
        Set<String> seen = new HashSet<>();
        for (User u : users) {
            if (seen.add(u.getUserId())) {
                notificationService.publish(u.getUserId(), type, title, body, entityType, entityId, severity);
            }
        }
    }
}
