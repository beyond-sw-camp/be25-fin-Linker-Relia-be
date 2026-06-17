package com.linker.relia.common.sse;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AlarmEvent {
    private final UUID receiverUserId;   // 받을 사람 userId (UUID)
    private final AlarmType type;
    private final String message;
    private final UUID referenceId;      // 관련 엔티티 ID (handoverId)
}