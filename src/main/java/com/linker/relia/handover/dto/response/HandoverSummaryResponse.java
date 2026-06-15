package com.linker.relia.handover.dto.response;

public record HandoverSummaryResponse(
        long pendingCount,
        long thisMonthCompletedCount,
        long thisMonthTotalCount
) {}
