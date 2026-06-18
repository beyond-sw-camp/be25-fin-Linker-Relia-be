package com.linker.relia.notification.service;

import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.notification.domain.Notification;
import com.linker.relia.notification.dto.response.NotificationListResponse;
import com.linker.relia.notification.exception.NotificationErrorCode;
import com.linker.relia.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(UUID userId) {
        List<Notification> notifications =
                notificationRepository.findByReceiverIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);

        long unreadCount =
                notificationRepository.countByReceiverIdAndReadYnFalseAndDeletedAtIsNull(userId);

        return NotificationListResponse.of(notifications, unreadCount);
    }

    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        // 본인 알림이 아닌데 읽음 처리 시도하는 경우 방지
        if (!notification.getReceiverId().equals(userId)) {
            throw new BusinessException(NotificationErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        notification.markAsRead();
        // JPA 더티체킹으로 자동 UPDATE
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> unread = notificationRepository
                .findByReceiverIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .filter(n -> !n.isReadYn())
                .toList();

        unread.forEach(Notification::markAsRead);
    }
}