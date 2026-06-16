package com.linker.relia.commission.repository.custom;

import com.linker.relia.commission.dto.OrganizationCommissionMonthlyTrendQueryResult;

import java.util.List;
import java.util.UUID;

public interface OrganizationCommissionTrendQueryRepository {
    List<OrganizationCommissionMonthlyTrendQueryResult> findBranchTrendQueryResults(String startMonth,
                                                                                    String endMonth,
                                                                                    UUID organizationId);

    List<OrganizationCommissionMonthlyTrendQueryResult> findHqTrendQueryResults(String startMonth,
                                                                                String endMonth);
}
