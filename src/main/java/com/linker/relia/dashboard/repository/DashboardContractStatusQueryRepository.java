package com.linker.relia.dashboard.repository;

import com.linker.relia.dashboard.dto.DashboardContractStatusQueryResult;

import java.util.UUID;

public interface DashboardContractStatusQueryRepository {
    DashboardContractStatusQueryResult summarizeContractStatuses(UUID fpId, String closingMonth);
}
