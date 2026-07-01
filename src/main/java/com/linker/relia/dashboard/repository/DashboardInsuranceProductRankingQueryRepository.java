package com.linker.relia.dashboard.repository;

import com.linker.relia.dashboard.dto.DashboardInsuranceProductRankingItemResponse;

import java.util.List;
import java.util.UUID;

public interface DashboardInsuranceProductRankingQueryRepository {
    List<DashboardInsuranceProductRankingItemResponse> findProductRankings(
            String closingMonth,
            UUID organizationId,
            UUID fpId,
            int limit
    );
}
