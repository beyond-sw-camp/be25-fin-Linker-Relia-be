package com.linker.relia.handover.dto.response;

import com.linker.relia.handover.domain.HandoverRequest;
import com.linker.relia.handover.domain.RequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record HandoverCreateResponse(
        UUID handoverRequestId,
        RequestStatus requestStatus,
        LocalDateTime createdAt
) {
    public static HandoverCreateResponse from(HandoverRequest handoverRequest) {
        return new HandoverCreateResponse(
                handoverRequest.getId(),
                handoverRequest.getRequestStatus(),
                handoverRequest.getCreatedAt()
        );
    }
}