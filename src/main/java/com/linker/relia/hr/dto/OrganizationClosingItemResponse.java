package com.linker.relia.hr.dto;

import com.linker.relia.hr.domain.OrganizationMonthlyClosing;
import com.linker.relia.organization.domain.OrganizationStatus;
import com.linker.relia.organization.domain.OrganizationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class OrganizationClosingItemResponse {
    private final UUID organizationId;
    private final String organizationCode;
    private final String organizationName;
    private final OrganizationType organizationType;
    private final OrganizationStatus organizationStatus;
    private final LocalDateTime closedAt;

    public static OrganizationClosingItemResponse from(OrganizationMonthlyClosing closing) {
        return OrganizationClosingItemResponse.builder()
                .organizationId(closing.getOrganization().getId())
                .organizationCode(closing.getOrganizationCode())
                .organizationName(closing.getOrganizationName())
                .organizationType(closing.getOrganizationType())
                .organizationStatus(closing.getOrganizationStatus())
                .closedAt(closing.getClosedAt())
                .build();
    }
}
