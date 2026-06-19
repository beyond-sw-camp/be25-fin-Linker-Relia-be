package com.linker.relia.notification.controller;

import com.linker.relia.notification.dto.response.NotificationListResponse;
import com.linker.relia.notification.service.NotificationService;
import com.linker.relia.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<NotificationListResponse> getNotifications(
            @AuthenticationPrincipal PrincipalDetails principal) {
        UUID userId = principal.getUser().getId();
        return ResponseEntity.ok(notificationService.getNotifications(userId));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal PrincipalDetails principal,
            @PathVariable UUID notificationId) {
        UUID userId = principal.getUser().getId();
        notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal PrincipalDetails principal) {
        UUID userId = principal.getUser().getId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }
}
