package com.linker.relia.organization.dto;

import com.linker.relia.organization.domain.OrganizationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganizationChartRequest {
    private OrganizationStatus status;
}
