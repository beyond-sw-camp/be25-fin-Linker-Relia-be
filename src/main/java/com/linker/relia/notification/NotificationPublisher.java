package com.linker.relia.notification;

import com.linker.relia.notification.domain.Notification;
import com.linker.relia.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final SseEmitterManager sseEmitterManager;
    private final NotificationRepository notificationRepository;

    @Transactional
    public void publish(NotificationEvent event) {
        Notification notification = Notification.builder()
                .receiverId(event.getReceiverUserId())
                .notificationType(event.getType())
                .content(event.getMessage())
                .targetType(event.getTargetType())
                .targetId(event.getReferenceId())
                .build();
        notificationRepository.save(notification);

        sendAfterCommit(event);
    }

    private void sendAfterCommit(NotificationEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sseEmitterManager.send(event.getReceiverUserId(), event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sseEmitterManager.send(event.getReceiverUserId(), event);
            }
        });
    }
}
