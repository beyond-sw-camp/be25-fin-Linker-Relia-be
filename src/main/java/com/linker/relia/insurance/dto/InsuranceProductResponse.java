package com.linker.relia.insurance.dto;

import com.linker.relia.insurance.domain.InsuranceProduct;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class InsuranceProductResponse {
    private final UUID insuranceProductId;
    private final String insuranceProductName;
    private final UUID insuranceCompanyId;
    private final String insuranceCompanyName;
    private final UUID insuranceCategoryId;
    private final String insuranceCategoryName;

    public static InsuranceProductResponse from(InsuranceProduct insuranceProduct) {
        return InsuranceProductResponse.builder()
                .insuranceProductId(insuranceProduct.getId())
                .insuranceProductName(insuranceProduct.getInsuranceProductName())
                .insuranceCompanyId(insuranceProduct.getInsuranceCompany().getId())
                .insuranceCompanyName(insuranceProduct.getInsuranceCompany().getInsuranceCompanyName())
                .insuranceCategoryId(insuranceProduct.getInsuranceCategory().getId())
                .insuranceCategoryName(insuranceProduct.getInsuranceCategory().getInsuranceCategoryName())
                .build();
    }
}
