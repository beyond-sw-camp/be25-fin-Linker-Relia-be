package com.linker.relia.dashboard.dto;

import com.linker.relia.common.dto.response.PageResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardBranchRankingResponse {
    private final String closingMonth;
    private final String comparisonClosingMonth;
    private final DashboardRankOrder rankOrder;
    private final PageResponse<DashboardBranchRankingItemResponse> rankings;
}
