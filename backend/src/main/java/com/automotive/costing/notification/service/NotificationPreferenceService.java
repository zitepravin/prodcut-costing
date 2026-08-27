package com.automotive.costing.notification.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.notification.entity.NotificationPreference;
import com.automotive.costing.notification.repository.NotificationPreferenceRepository;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.access.service.CentralAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Wave F — user notification preferences.
 * Preferences for a module are only valid when the tenant is entitled to that module.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final CentralAccessService centralAccessService;

    @Transactional(readOnly = true)
    public List<NotificationPreference> listForUser(String userId) {
        return preferenceRepository.findByUserIdOrderByModuleCodeAscEventTypeAsc(userId);
    }

    /**
     * @return true if email may be sent (default true when no preference row)
     */
    @Transactional(readOnly = true)
    public boolean isEmailEnabled(String userId, String moduleCode, String eventType) {
        return preferenceRepository.findByUserIdAndModuleCodeAndEventType(userId, moduleCode, eventType)
                .map(NotificationPreference::isEmailEnabled)
                .orElse(true);
    }

    @Transactional
    public NotificationPreference upsert(
            String userId,
            String moduleCode,
            String eventType,
            boolean emailEnabled,
            boolean inAppEnabled
    ) {
        String mod = moduleCode == null ? "" : moduleCode.trim().toUpperCase(Locale.ROOT);
        String evt = eventType == null ? "" : eventType.trim().toUpperCase(Locale.ROOT);
        if (mod.isBlank() || evt.isBlank()) {
            throw new BusinessRuleViolationException("moduleCode and eventType are required", "PREF_REQUIRED");
        }

        String customerId = CustomerContext.getCustomerId();
        if (customerId != null && !customerId.isBlank()) {
            // Enforce module entitlement for preference of that module
            try {
                centralAccessService.assertModuleEnabled(customerId, mod);
            } catch (RuntimeException ex) {
                throw new BusinessRuleViolationException(
                        "Cannot configure notifications for unsubscribed module " + mod,
                        "PREF_MODULE_NOT_ENTITLED");
            }
        }

        NotificationPreference pref = preferenceRepository
                .findByUserIdAndModuleCodeAndEventType(userId, mod, evt)
                .orElseGet(() -> {
                    NotificationPreference n = new NotificationPreference();
                    n.setPreferenceId(UUID.randomUUID().toString());
                    n.setUserId(userId);
                    n.setModuleCode(mod);
                    n.setEventType(evt);
                    n.setCreatedAt(Instant.now());
                    return n;
                });
        pref.setCustomerId(customerId);
        pref.setEmailEnabled(emailEnabled);
        pref.setInAppEnabled(inAppEnabled);
        pref.setUpdatedAt(Instant.now());
        return preferenceRepository.save(pref);
    }
}
