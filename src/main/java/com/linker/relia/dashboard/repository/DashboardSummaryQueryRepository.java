package com.linker.relia.dashboard.repository;

import com.linker.relia.dashboard.dto.DashboardSummaryQueryResult;

import java.time.LocalDate;
import java.util.UUID;

public interface DashboardSummaryQueryRepository {
    DashboardSummaryQueryResult findFpSummary(UUID fpId,
                                              UUID organizationId,
                                              LocalDate currentMonthStart,
                                              LocalDate referenceDate,
                                              String currentMonth,
                                              String comparisonClosingMonth);
}
