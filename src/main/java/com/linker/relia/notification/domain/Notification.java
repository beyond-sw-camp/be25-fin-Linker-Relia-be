package com.linker.relia.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "receiver_id", length = 36, nullable = false)
    private UUID receiverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 30, nullable = false)
    private NotificationType notificationType;

    @Column(name = "content", length = 500, nullable = false)
    private String content;

    @Column(name = "target_type", length = 30)
    private String targetType;     // ex) "HANDOVER"

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "target_id", length = 36)
    private UUID targetId;         // ex) handoverId

    @Column(name = "read_yn", nullable = false)
    private boolean readYn;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private Notification(UUID receiverId, NotificationType notificationType, String content,
                         String targetType, UUID targetId) {
        this.id = UUID.randomUUID();
        this.receiverId = receiverId;
        this.notificationType = notificationType;
        this.content = content;
        this.targetType = targetType;
        this.targetId = targetId;
        this.readYn = false;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsRead() {
        this.readYn = true;
        this.readAt = LocalDateTime.now();
    }
}