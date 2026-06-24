package com.linker.relia.notification.repository;

import com.linker.relia.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // 목록 조회용 — 본인 알림만, 삭제 안 된 것만, 최신순
    List<Notification> findByReceiverIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID receiverId);

    // 안읽은 개수 카운트용
    long countByReceiverIdAndReadYnFalseAndDeletedAtIsNull(UUID receiverId);
}