package com.linker.relia.dashboard.repository;

import com.linker.relia.dashboard.dto.DashboardMonthlyContractCustomerTrendQueryResult;

import java.util.List;
import java.util.UUID;

public interface DashboardMonthlyContractCustomerTrendQueryRepository {
    List<DashboardMonthlyContractCustomerTrendQueryResult> findMonthlyContractCustomerTrends(
            UUID fpId,
            String startMonth,
            String endMonth
    );
}
