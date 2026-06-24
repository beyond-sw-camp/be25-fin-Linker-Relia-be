package com.linker.relia.notification.dto.response;

import com.linker.relia.notification.domain.Notification;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class NotificationResponse {
    private UUID id;
    private String notificationType;
    private String content;
    private String targetType;
    private UUID targetId;
    private boolean readYn;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .notificationType(notification.getNotificationType().name())
                .content(notification.getContent())
                .targetType(notification.getTargetType())
                .targetId(notification.getTargetId())
                .readYn(notification.isReadYn())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}