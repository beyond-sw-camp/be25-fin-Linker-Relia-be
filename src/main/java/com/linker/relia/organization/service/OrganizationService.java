package com.linker.relia.organization.service;

import com.linker.relia.organization.dto.OrganizationChartRequest;
import com.linker.relia.organization.dto.OrganizationChartResponse;

public interface OrganizationService {
    OrganizationChartResponse getOrganizationChart(OrganizationChartRequest request);
}
