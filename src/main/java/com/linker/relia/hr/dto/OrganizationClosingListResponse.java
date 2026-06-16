package com.linker.relia.hr.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OrganizationClosingListResponse {
    private final String closingMonth;
    private final int count;
    private final List<OrganizationClosingItemResponse> organizations;

    public static OrganizationClosingListResponse of(String closingMonth,
                                                     List<OrganizationClosingItemResponse> organizations) {
        return OrganizationClosingListResponse.builder()
                .closingMonth(closingMonth)
                .count(organizations.size())
                .organizations(organizations)
                .build();
    }
}
