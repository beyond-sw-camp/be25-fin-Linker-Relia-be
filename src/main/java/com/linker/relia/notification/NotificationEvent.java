package com.linker.relia.notification;

import com.linker.relia.notification.domain.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class NotificationEvent {
    private final UUID receiverUserId;
    private final NotificationType type;
    private final String message;
    private final String targetType;   // ex) "HANDOVER"  ← 추가
    private final UUID referenceId;  // = targetId
}