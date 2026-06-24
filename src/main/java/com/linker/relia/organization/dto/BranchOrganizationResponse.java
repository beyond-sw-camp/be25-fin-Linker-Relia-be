package com.linker.relia.organization.dto;

import com.linker.relia.organization.domain.Organization;
import com.linker.relia.organization.domain.OrganizationStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class BranchOrganizationResponse {
    private final UUID organizationId;
    private final String organizationCode;
    private final String organizationName;
    private final String organizationAddress;
    private final String organizationPhone;
    private final OrganizationStatus organizationStatus;
    private final long advisorCount;

    public BranchOrganizationResponse(UUID organizationId,
                                      String organizationCode,
                                      String organizationName,
                                      String organizationAddress,
                                      String organizationPhone,
                                      OrganizationStatus organizationStatus,
                                      long advisorCount) {
        this.organizationId = organizationId;
        this.organizationCode = organizationCode;
        this.organizationName = organizationName;
        this.organizationAddress = organizationAddress;
        this.organizationPhone = organizationPhone;
        this.organizationStatus = organizationStatus;
        this.advisorCount = advisorCount;
    }

    public static BranchOrganizationResponse from(Organization organization) {
        return BranchOrganizationResponse.builder()
                .organizationId(organization.getId())
                .organizationCode(organization.getOrganizationCode())
                .organizationName(organization.getOrganizationName())
                .organizationAddress(organization.getOrganizationAddress())
                .organizationPhone(organization.getOrganizationPhone())
                .organizationStatus(organization.getOrganizationStatus())
                .advisorCount(0)
                .build();
    }
}
