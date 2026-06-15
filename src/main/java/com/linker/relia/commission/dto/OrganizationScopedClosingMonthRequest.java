package com.linker.relia.commission.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganizationScopedClosingMonthRequest extends ClosingMonthRequest {
    private String organizationCode;
}
