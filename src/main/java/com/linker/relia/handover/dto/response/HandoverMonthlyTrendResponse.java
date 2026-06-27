package com.linker.relia.handover.dto.response;

public record HandoverMonthlyTrendResponse(
        String yearMonth,
        long requestCount
) {
}
