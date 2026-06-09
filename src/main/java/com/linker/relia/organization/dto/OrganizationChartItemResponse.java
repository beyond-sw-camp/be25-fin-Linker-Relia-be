package com.linker.relia.organization.dto;

import com.linker.relia.organization.domain.Organization;
import com.linker.relia.organization.domain.OrganizationStatus;
import com.linker.relia.organization.domain.OrganizationType;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class OrganizationChartItemResponse {
    private final UUID id;
    private final String organizationCode;
    private final UUID parentOrganizationId;
    private final String organizationName;
    private final OrganizationType organizationType;
    private final String organizationAddress;
    private final String organizationPhone;
    private final OrganizationStatus organizationStatus;
    @Builder.Default
    private final List<OrganizationChartItemResponse> children = new ArrayList<>();

    public static OrganizationChartItemResponse from(Organization organization) {
        return OrganizationChartItemResponse.builder()
                .id(organization.getId())
                .organizationCode(organization.getOrganizationCode())
                .parentOrganizationId(organization.getParentOrganizationId())
                .organizationName(organization.getOrganizationName())
                .organizationType(organization.getOrganizationType())
                .organizationAddress(organization.getOrganizationAddress())
                .organizationPhone(organization.getOrganizationPhone())
                .organizationStatus(organization.getOrganizationStatus())
                .build();
    }
}
