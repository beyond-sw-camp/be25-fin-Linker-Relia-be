package com.linker.relia.organization.service;

import com.linker.relia.organization.dto.BranchOrganizationResponse;
import com.linker.relia.organization.dto.FpContractListRequest;
import com.linker.relia.organization.dto.FpContractListResponse;
import com.linker.relia.organization.dto.FpDetailResponse;
import com.linker.relia.organization.dto.FpListRequest;
import com.linker.relia.organization.dto.FpListResponse;
import com.linker.relia.organization.dto.FpMonthlyPerformanceResponse;
import com.linker.relia.organization.dto.OrganizationChartRequest;
import com.linker.relia.organization.dto.OrganizationChartResponse;
import com.linker.relia.security.principal.PrincipalDetails;

import java.util.List;
import java.util.UUID;

public interface OrganizationService {
    OrganizationChartResponse getOrganizationChart(OrganizationChartRequest request);

    List<BranchOrganizationResponse> getBranchOrganizations();

    FpListResponse getFps(PrincipalDetails principalDetails, FpListRequest request);

    FpDetailResponse getFpDetail(PrincipalDetails principalDetails, UUID fpId, String closingMonth);

    FpContractListResponse getFpContracts(PrincipalDetails principalDetails,
                                          UUID fpId,
                                          FpContractListRequest request);

    FpMonthlyPerformanceResponse getFpMonthlyPerformances(PrincipalDetails principalDetails,
                                                          UUID fpId,
                                                          String fromClosingMonth,
                                                          String toClosingMonth);
}
