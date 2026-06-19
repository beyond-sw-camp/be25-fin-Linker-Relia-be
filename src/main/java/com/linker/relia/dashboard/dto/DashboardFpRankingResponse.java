package com.linker.relia.dashboard.dto;

import com.linker.relia.common.dto.response.PageResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardFpRankingResponse {
    private final String closingMonth;
    private final String organizationCode;
    private final String organizationName;
    private final DashboardRankOrder rankOrder;
    private final PageResponse<DashboardFpRankingItemResponse> rankings;
}
