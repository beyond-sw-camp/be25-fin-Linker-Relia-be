package com.linker.relia.dashboard.dto;

import com.linker.relia.common.access.AccessScopeType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DashboardInsuranceProductRankingResponse {
    private final String closingMonth;
    private final AccessScopeType scopeType;
    private final String organizationCode;
    private final String organizationName;
    private final int limit;
    private final List<DashboardInsuranceProductRankingItemResponse> rankings;
}
