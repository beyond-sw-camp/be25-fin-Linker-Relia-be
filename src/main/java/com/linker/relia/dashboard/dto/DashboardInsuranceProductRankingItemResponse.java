package com.linker.relia.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardInsuranceProductRankingItemResponse {
    private final String insuranceProductName;
    private final String insuranceCompanyName;
    private final long contractCount;
}
