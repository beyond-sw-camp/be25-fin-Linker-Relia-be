package com.linker.relia.dashboard.service;

import com.linker.relia.dashboard.dto.DashboardClosingMonthOptionResponse;
import com.linker.relia.dashboard.dto.DashboardKpiRequest;
import com.linker.relia.dashboard.dto.FpDashboardContractStatusResponse;
import com.linker.relia.dashboard.dto.FpDashboardContractDistributionResponse;
import com.linker.relia.dashboard.dto.FpDashboardMonthlyCommissionTrendResponse;
import com.linker.relia.dashboard.dto.FpDashboardMonthlyContractCustomerTrendResponse;
import com.linker.relia.dashboard.dto.FpDashboardSummaryResponse;
import com.linker.relia.dashboard.dto.OrganizationDashboardKpiResponse;
import com.linker.relia.security.principal.PrincipalDetails;

import java.time.LocalDate;
import java.util.List;

public interface DashboardService {
    List<DashboardClosingMonthOptionResponse> getClosingMonthOptions();

    OrganizationDashboardKpiResponse getOrganizationKpi(
            PrincipalDetails principalDetails,
            DashboardKpiRequest request
    );

    FpDashboardSummaryResponse getFpSummary(PrincipalDetails principalDetails, LocalDate referenceDate);

    FpDashboardContractStatusResponse getFpContractStatus(PrincipalDetails principalDetails, LocalDate referenceDate);

    FpDashboardContractDistributionResponse getFpContractDistribution(
            PrincipalDetails principalDetails,
            LocalDate referenceDate
    );

    FpDashboardMonthlyContractCustomerTrendResponse getFpMonthlyContractCustomerTrend(
            PrincipalDetails principalDetails,
            LocalDate referenceDate
    );

    FpDashboardMonthlyCommissionTrendResponse getFpMonthlyCommissionTrend(
            PrincipalDetails principalDetails,
            LocalDate referenceDate
    );
}
