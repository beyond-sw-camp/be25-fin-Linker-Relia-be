package com.linker.relia.organization.dto;

import com.linker.relia.organization.domain.Organization;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class BranchOrganizationResponse {
    private final UUID organizationId;
    private final String organizationCode;
    private final String organizationName;

    public static BranchOrganizationResponse from(Organization organization) {
        return BranchOrganizationResponse.builder()
                .organizationId(organization.getId())
                .organizationCode(organization.getOrganizationCode())
                .organizationName(organization.getOrganizationName())
                .build();
    }
}
