package com.automotive.costing.notification.repository;

import com.automotive.costing.notification.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, String> {

    Optional<NotificationTemplate> findFirstByEventTypeAndLanguageCodeAndActiveFlagTrue(
            String eventType, String languageCode);

    Optional<NotificationTemplate> findByTemplateCodeAndLanguageCode(String templateCode, String languageCode);

    List<NotificationTemplate> findByActiveFlagTrueOrderByEventTypeAsc();
}
