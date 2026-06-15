package com.linker.relia.handover.dto.response;

public record HandoverReceivedSummaryResponse(
        long thisMonthReceivedCount,  // 이번 달 인수
        long totalReceivedCount,      // 누적 인수 건수
        double successRate            // 인수 성공률 (%)
) {}