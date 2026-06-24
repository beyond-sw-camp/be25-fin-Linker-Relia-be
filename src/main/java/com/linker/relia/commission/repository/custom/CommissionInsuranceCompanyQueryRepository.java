package com.linker.relia.commission.repository.custom;

import com.linker.relia.commission.dto.InsuranceCompanyCommissionSummaryQueryResult;

import java.util.List;
import java.util.UUID;

public interface CommissionInsuranceCompanyQueryRepository {
    List<InsuranceCompanyCommissionSummaryQueryResult> findFpSummaries(String closingMonth, UUID fpId);

    List<InsuranceCompanyCommissionSummaryQueryResult> findBranchSummaries(String closingMonth, UUID organizationId);

    List<InsuranceCompanyCommissionSummaryQueryResult> findHqSummaries(String closingMonth);
}
