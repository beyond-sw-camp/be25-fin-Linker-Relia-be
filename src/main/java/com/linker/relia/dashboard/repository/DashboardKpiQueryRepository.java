package com.linker.relia.dashboard.repository;

import com.linker.relia.dashboard.dto.DashboardKpiQueryResult;

import java.util.UUID;

public interface DashboardKpiQueryRepository {
    DashboardKpiQueryResult findHqKpi(String closingMonth);

    DashboardKpiQueryResult findBranchKpi(UUID organizationId, String closingMonth);
}
