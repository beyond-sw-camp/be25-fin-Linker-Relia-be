package com.linker.relia.dashboard.repository;

import com.linker.relia.dashboard.dto.DashboardMonthlyCommissionTrendQueryResult;

import java.util.List;
import java.util.UUID;

public interface DashboardMonthlyCommissionTrendQueryRepository {
    List<DashboardMonthlyCommissionTrendQueryResult> findMonthlyCommissionTrends(
            UUID fpId,
            String startMonth,
            String endMonth
    );
}
