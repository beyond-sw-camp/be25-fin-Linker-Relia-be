package com.linker.relia.customer.dto;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class CustomerFpHistoryItemResponse {
    private final UUID historyId;
    private final Integer customerFpSequence;
    private final LocalDateTime changedAt;
    private final String beforeFpName;
    private final String afterFpName;
    private final String changedReason;

    public CustomerFpHistoryItemResponse(UUID historyId,
                                         Integer customerFpSequence,
                                         LocalDateTime changedAt,
                                         String beforeFpName,
                                         String afterFpName,
                                         String changedReason) {
        this.historyId = historyId;
        this.customerFpSequence = customerFpSequence;
        this.changedAt = changedAt;
        this.beforeFpName = beforeFpName;
        this.afterFpName = afterFpName;
        this.changedReason = changedReason;
    }
}
