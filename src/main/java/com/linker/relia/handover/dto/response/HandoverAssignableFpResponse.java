package com.linker.relia.handover.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record HandoverAssignableFpResponse(
        UUID fpId,
        String fpName,
        Integer careerYears,
        String specialtyCategory,
        Integer preferredCustomerAge,
        String consultationChannel,
        long customerCount,
        long contractCount,
        BigDecimal retentionRate
) {}
