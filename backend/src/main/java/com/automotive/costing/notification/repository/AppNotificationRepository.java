package com.automotive.costing.notification.repository;

import com.automotive.costing.notification.entity.AppNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppNotificationRepository extends JpaRepository<AppNotification, String> {

    Page<AppNotification> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId, Pageable pageable);

    Page<AppNotification> findByRecipientUserIdAndReadFlagOrderByCreatedAtDesc(
            String recipientUserId, boolean readFlag, Pageable pageable);

    long countByRecipientUserIdAndReadFlag(String recipientUserId, boolean readFlag);
}
