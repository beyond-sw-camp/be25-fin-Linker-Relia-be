package com.linker.relia.commission.repository;

import com.linker.relia.commission.dto.InsuranceCompanyCommissionSummaryRow;

import java.util.List;
import java.util.UUID;

public interface CommissionInsuranceCompanyQueryRepository {
    List<InsuranceCompanyCommissionSummaryRow> findFpSummaries(String closingMonth, UUID fpId);

    List<InsuranceCompanyCommissionSummaryRow> findBranchSummaries(String closingMonth, UUID organizationId);

    List<InsuranceCompanyCommissionSummaryRow> findHqSummaries(String closingMonth);
}
