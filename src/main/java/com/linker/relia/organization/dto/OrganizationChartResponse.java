package com.linker.relia.organization.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OrganizationChartResponse {
    private final List<OrganizationChartItemResponse> organizations;
}
