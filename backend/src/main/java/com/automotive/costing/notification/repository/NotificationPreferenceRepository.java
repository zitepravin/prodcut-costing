package com.automotive.costing.notification.repository;

import com.automotive.costing.notification.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, String> {

    List<NotificationPreference> findByUserIdOrderByModuleCodeAscEventTypeAsc(String userId);

    Optional<NotificationPreference> findByUserIdAndModuleCodeAndEventType(
            String userId, String moduleCode, String eventType);
}
