package com.linker.relia.commission.service;

import com.linker.relia.commission.dto.CommissionPaymentTypeSummaryResponse;
import com.linker.relia.commission.dto.FpCommissionListRequest;
import com.linker.relia.commission.dto.FpCommissionMonthlyTrendResponse;
import com.linker.relia.commission.dto.FpCommissionListResponse;
import com.linker.relia.commission.dto.FpCommissionSummaryRequest;
import com.linker.relia.commission.dto.FpCommissionSummaryResponse;
import com.linker.relia.commission.dto.InsuranceCompanyCommissionSummaryResponse;
import com.linker.relia.commission.dto.OrganizationCommissionMonthlyTrendResponse;
import com.linker.relia.commission.dto.OrganizationCommissionSummaryResponse;
import com.linker.relia.commission.dto.OrganizationScopedClosingMonthRequest;
import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.security.principal.PrincipalDetails;

import java.util.List;

public interface CommissionService {
    PageResponse<FpCommissionListResponse> getFpCommissionList(PrincipalDetails principalDetails,
                                                               FpCommissionListRequest request);

    FpCommissionSummaryResponse getFpCommissionSummary(PrincipalDetails principalDetails, FpCommissionSummaryRequest request);

    List<FpCommissionMonthlyTrendResponse> getFpCommissionTrend(PrincipalDetails principalDetails);

    OrganizationCommissionSummaryResponse getOrganizationCommissionSummary(PrincipalDetails principalDetails,
                                                                           OrganizationScopedClosingMonthRequest request);

    List<OrganizationCommissionMonthlyTrendResponse> getOrganizationCommissionTrend(PrincipalDetails principalDetails,
                                                                                    String organizationCode);

    CommissionPaymentTypeSummaryResponse getCommissionPaymentTypeSummary(PrincipalDetails principalDetails,
                                                                         OrganizationScopedClosingMonthRequest request);

    InsuranceCompanyCommissionSummaryResponse getInsuranceCompanyCommissionSummary(PrincipalDetails principalDetails,
                                                                                   OrganizationScopedClosingMonthRequest request);
}
