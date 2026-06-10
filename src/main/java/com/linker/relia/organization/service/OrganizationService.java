package com.linker.relia.organization.service;

import com.linker.relia.organization.dto.BranchOrganizationResponse;
import com.linker.relia.organization.dto.OrganizationChartRequest;
import com.linker.relia.organization.dto.OrganizationChartResponse;

import java.util.List;

public interface OrganizationService {
    OrganizationChartResponse getOrganizationChart(OrganizationChartRequest request);

    List<BranchOrganizationResponse> getBranchOrganizations();
}
