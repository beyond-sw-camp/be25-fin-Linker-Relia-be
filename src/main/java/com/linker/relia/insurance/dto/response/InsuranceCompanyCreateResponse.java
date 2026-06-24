package com.linker.relia.insurance.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class InsuranceCompanyCreateResponse {
    private final UUID insuranceCompanyId;
    private final String insuranceCompanyCode;
    private final String insuranceCompanyName;
}
