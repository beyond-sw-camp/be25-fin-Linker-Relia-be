package com.linker.relia.notification.dto.response;

import com.linker.relia.notification.domain.Notification;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NotificationListResponse {
    private List<NotificationResponse> notifications;
    private long unreadCount;

    public static NotificationListResponse of(List<Notification> notifications, long unreadCount) {
        return NotificationListResponse.builder()
                .notifications(notifications.stream()
                        .map(NotificationResponse::from)
                        .toList())
                .unreadCount(unreadCount)
                .build();
    }
}