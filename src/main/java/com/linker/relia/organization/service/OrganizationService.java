package com.linker.relia.organization.service;

import com.linker.relia.organization.dto.BranchOrganizationResponse;
import com.linker.relia.organization.dto.FpListRequest;
import com.linker.relia.organization.dto.FpListResponse;
import com.linker.relia.organization.dto.OrganizationChartRequest;
import com.linker.relia.organization.dto.OrganizationChartResponse;
import com.linker.relia.security.principal.PrincipalDetails;

import java.util.List;

public interface OrganizationService {
    /**
 * Retrieves the organization's hierarchical chart according to the specified request.
 *
 * @param request criteria and options used to build or filter the organization chart
 * @return an OrganizationChartResponse containing the hierarchical structure and related metadata
 */
OrganizationChartResponse getOrganizationChart(OrganizationChartRequest request);

    /**
 * Retrieves the list of branch organizations.
 *
 * @return a list of BranchOrganizationResponse objects representing each branch organization
 */
List<BranchOrganizationResponse> getBranchOrganizations();

    /**
 * Retrieve a list of financial partners (FPs) filtered and paginated according to the provided criteria,
 * taking the caller's identity and authorization context into account.
 *
 * @param principalDetails the caller's identity and authorization context used to scope the results
 * @param request          filtering and pagination criteria for the FP list
 * @return                 an FpListResponse containing matching FP entries and pagination metadata
 */
FpListResponse getFps(PrincipalDetails principalDetails, FpListRequest request);
}
