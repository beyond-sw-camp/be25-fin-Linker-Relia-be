package com.linker.relia.commission.service;

import com.linker.relia.commission.dto.CommissionPaymentTypeSummaryRequest;
import com.linker.relia.commission.dto.CommissionPaymentTypeSummaryResponse;
import com.linker.relia.commission.dto.FpCommissionSummaryRequest;
import com.linker.relia.commission.dto.FpCommissionSummaryResponse;
import com.linker.relia.commission.dto.OrganizationCommissionSummaryRequest;
import com.linker.relia.commission.dto.OrganizationCommissionSummaryResponse;
import com.linker.relia.security.principal.PrincipalDetails;

public interface CommissionService {
    FpCommissionSummaryResponse getFpCommissionSummary(PrincipalDetails principalDetails, FpCommissionSummaryRequest request);

    OrganizationCommissionSummaryResponse getOrganizationCommissionSummary(PrincipalDetails principalDetails, OrganizationCommissionSummaryRequest request);

    CommissionPaymentTypeSummaryResponse getCommissionPaymentTypeSummary(PrincipalDetails principalDetails,
                                                                        CommissionPaymentTypeSummaryRequest request);
}
