package com.linker.relia.dashboard.repository;

import com.linker.relia.dashboard.dto.DashboardContractDistributionQueryResult;

import java.util.List;
import java.util.UUID;

public interface DashboardContractDistributionQueryRepository {
    List<DashboardContractDistributionQueryResult> summarizeInsuranceCompanyContractCounts(
            UUID fpId,
            String closingMonth
    );

    List<DashboardContractDistributionQueryResult> summarizeInsuranceCategoryContractCounts(
            UUID fpId,
            String closingMonth
    );
}
