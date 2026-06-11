package com.linker.relia.insurance.dto;

import com.linker.relia.insurance.domain.InsuranceCompany;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class InsuranceCompanyResponse {
    private final UUID insuranceCompanyId;
    private final String insuranceCompanyCode;
    private final String insuranceCompanyName;

    public static InsuranceCompanyResponse from(InsuranceCompany insuranceCompany) {
        return InsuranceCompanyResponse.builder()
                .insuranceCompanyId(insuranceCompany.getId())
                .insuranceCompanyCode(insuranceCompany.getInsuranceCompanyCode())
                .insuranceCompanyName(insuranceCompany.getInsuranceCompanyName())
                .build();
    }
}
