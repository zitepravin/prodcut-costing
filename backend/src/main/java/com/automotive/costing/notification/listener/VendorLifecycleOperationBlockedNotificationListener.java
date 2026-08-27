package com.automotive.costing.notification.listener;

import com.automotive.costing.common.event.SupplierLifecycleOperationBlockedEvent;
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
 * Notifies sourcing roles when scorecard/TCO is blocked by lifecycle.
 * Does not reinstate or approve suppliers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VendorLifecycleOperationBlockedNotificationListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBlocked(SupplierLifecycleOperationBlockedEvent event) {
        String title = "Vendor operation blocked (" + event.operationCode() + ")";
        String body = "Supplier " + event.supplierId() + " lifecycle " + event.lifecycleStatus()
                + " refused " + event.operationCode() + ". " + event.blockReason()
                + " System does not auto-reinstate.";
        for (String role : List.of("SOURCING_LEAD", "COSTING_MANAGER", "PURCHASING_MANAGER")) {
            notifyRole(role, title, body, event.supplierId());
        }
        log.info("Lifecycle operation block notification for supplier {} op={}",
                event.supplierId(), event.operationCode());
    }

    private void notifyRole(String roleCode, String title, String body, String supplierId) {
        List<User> users = userRepository.findActiveByRoleCode(roleCode);
        Set<String> seen = new HashSet<>();
        for (User u : users) {
            if (seen.add(u.getUserId())) {
                notificationService.publish(
                        u.getUserId(),
                        NotificationType.VENDOR_LIFECYCLE_OPERATION_BLOCKED,
                        title,
                        body,
                        "SupplierMaster",
                        supplierId,
                        NotificationSeverity.WARNING);
            }
        }
    }
}
