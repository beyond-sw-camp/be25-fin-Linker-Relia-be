package com.linker.relia.commission.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class OrganizationScopedClosingMonthRequest extends ClosingMonthRequest {
    private String organizationCode;
}
