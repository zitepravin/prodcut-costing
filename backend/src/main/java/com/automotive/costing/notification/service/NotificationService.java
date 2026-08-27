package com.automotive.costing.notification.service;

import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.notification.dto.NotificationDtos.Response;
import com.automotive.costing.notification.dto.NotificationDtos.UnreadCountResponse;
import com.automotive.costing.notification.entity.AppNotification;
import com.automotive.costing.notification.entity.NotificationSeverity;
import com.automotive.costing.notification.entity.NotificationType;
import com.automotive.costing.notification.repository.AppNotificationRepository;
import com.automotive.costing.platform.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final AppNotificationRepository repository;
    private final NotificationDeliveryService deliveryService;

    @Transactional
    public AppNotification publish(
            String recipientUserId,
            NotificationType type,
            String title,
            String body,
            String entityType,
            String entityId,
            NotificationSeverity severity) {

        AppNotification n = new AppNotification();
        n.setNotificationId(UUID.randomUUID().toString());
        n.setRecipientUserId(recipientUserId);
        n.setNotificationType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setEntityType(entityType);
        n.setEntityId(entityId);
        n.setSeverity(severity != null ? severity : NotificationSeverity.INFO);
        n.setReadFlag(false);
        AppNotification saved = repository.save(n);
        log.debug("Notification {} → user {} type {}", saved.getNotificationId(), recipientUserId, type);
        deliveryService.onPublished(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Response> listForUser(User user, Boolean unreadOnly, Pageable pageable) {
        Page<AppNotification> page;
        if (Boolean.TRUE.equals(unreadOnly)) {
            page = repository.findByRecipientUserIdAndReadFlagOrderByCreatedAtDesc(
                    user.getUserId(), false, pageable);
        } else {
            page = repository.findByRecipientUserIdOrderByCreatedAtDesc(user.getUserId(), pageable);
        }
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(User user) {
        return new UnreadCountResponse(repository.countByRecipientUserIdAndReadFlag(user.getUserId(), false));
    }

    @Transactional
    public Response markRead(User user, String notificationId) {
        AppNotification n = repository.findById(notificationId)
                .orElseThrow(() -> ResourceNotFoundException.of("AppNotification", notificationId));
        if (!n.getRecipientUserId().equals(user.getUserId())) {
            throw ResourceNotFoundException.of("AppNotification", notificationId);
        }
        if (!n.isReadFlag()) {
            n.setReadFlag(true);
            n.setReadAt(Instant.now());
            repository.save(n);
        }
        return toResponse(n);
    }

    @Transactional
    public int markAllRead(User user) {
        Page<AppNotification> unread = repository.findByRecipientUserIdAndReadFlagOrderByCreatedAtDesc(
                user.getUserId(), false, Pageable.unpaged());
        Instant now = Instant.now();
        int count = 0;
        for (AppNotification n : unread) {
            n.setReadFlag(true);
            n.setReadAt(now);
            repository.save(n);
            count++;
        }
        return count;
    }

    private Response toResponse(AppNotification n) {
        return new Response(
                n.getNotificationId(),
                n.getNotificationType(),
                n.getTitle(),
                n.getBody(),
                n.getEntityType(),
                n.getEntityId(),
                n.getSeverity(),
                n.isReadFlag(),
                n.getReadAt(),
                n.getCreatedAt());
    }
}
