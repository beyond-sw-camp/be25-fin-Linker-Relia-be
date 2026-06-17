package com.linker.relia.dashboard.repository;

import com.linker.relia.dashboard.dto.DashboardSummaryQueryResult;

import java.util.UUID;

public interface DashboardSummaryQueryRepository {
    DashboardSummaryQueryResult findFpSummary(UUID fpId,
                                              String closingMonth,
                                              String comparisonClosingMonth);
}
