package com.automotive.costing.notification.controller;

import com.automotive.costing.notification.dto.NotificationDtos.Response;
import com.automotive.costing.notification.dto.NotificationDtos.UnreadCountResponse;
import com.automotive.costing.notification.service.NotificationService;
import com.automotive.costing.platform.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List my notifications (newest first)")
    public ResponseEntity<Page<Response>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Boolean unreadOnly,
            Pageable pageable) {
        return ResponseEntity.ok(notificationService.listForUser(user, unreadOnly, pageable));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Unread notification count for badge")
    public ResponseEntity<UnreadCountResponse> unreadCount(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.unreadCount(user));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark one notification as read")
    public ResponseEntity<Response> markRead(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        return ResponseEntity.ok(notificationService.markRead(user, id));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all my notifications as read")
    public ResponseEntity<UnreadCountResponse> markAllRead(@AuthenticationPrincipal User user) {
        int marked = notificationService.markAllRead(user);
        return ResponseEntity.ok(new UnreadCountResponse(0));
    }
}
