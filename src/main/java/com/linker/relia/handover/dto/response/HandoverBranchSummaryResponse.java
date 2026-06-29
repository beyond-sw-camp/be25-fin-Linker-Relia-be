package com.linker.relia.handover.dto.response;

import java.util.UUID;

/**
 * 지점별 현황 요약 테이블 한 줄.
 * completionRate, pendingRate는 소수 1자리로 반올림해서 내려줌.
 */
public record HandoverBranchSummaryResponse(
        UUID organizationId,
        String organizationName,
        long totalCount,
        long completedCount,
        double completionRate,
        long pendingCount,
        double pendingRate
) {

    public static HandoverBranchSummaryResponse rounded(HandoverBranchSummaryResponse raw) {
        return new HandoverBranchSummaryResponse(
                raw.organizationId(),
                raw.organizationName(),
                raw.totalCount(),
                raw.completedCount(),
                Math.round(raw.completionRate() * 10) / 10.0,
                raw.pendingCount(),
                Math.round(raw.pendingRate() * 10) / 10.0
        );
    }
}