package com.linker.relia.commission.service;

import com.linker.relia.commission.dto.CommissionPaymentTypeSummaryResponse;
import com.linker.relia.commission.dto.FpCommissionSummaryRequest;
import com.linker.relia.commission.dto.FpCommissionSummaryResponse;
import com.linker.relia.commission.dto.InsuranceCompanyCommissionSummaryResponse;
import com.linker.relia.commission.dto.OrganizationCommissionSummaryResponse;
import com.linker.relia.commission.dto.OrganizationScopedClosingMonthRequest;
import com.linker.relia.security.principal.PrincipalDetails;

public interface CommissionService {
    FpCommissionSummaryResponse getFpCommissionSummary(PrincipalDetails principalDetails, FpCommissionSummaryRequest request);

    OrganizationCommissionSummaryResponse getOrganizationCommissionSummary(PrincipalDetails principalDetails,
                                                                           OrganizationScopedClosingMonthRequest request);

    CommissionPaymentTypeSummaryResponse getCommissionPaymentTypeSummary(PrincipalDetails principalDetails,
                                                                         OrganizationScopedClosingMonthRequest request);

    InsuranceCompanyCommissionSummaryResponse getInsuranceCompanyCommissionSummary(PrincipalDetails principalDetails,
                                                                                   OrganizationScopedClosingMonthRequest request);
}
