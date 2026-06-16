package com.linker.relia.handover.dto.response;

import com.linker.relia.customer.domain.CustomerGrade;

import java.time.LocalDateTime;
import java.util.UUID;

public record HandoverReceivedItemResponse(
        UUID handoverRequestId,
        UUID customerId,
        String customerName,
        CustomerGrade customerGrade,
        String beforeFpName,
        String changedReason,
        LocalDateTime changedAt
) {}
