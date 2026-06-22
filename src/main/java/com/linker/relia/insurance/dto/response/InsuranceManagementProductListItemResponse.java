package com.linker.relia.insurance.dto.response;

import com.linker.relia.insurance.domain.InsuranceProduct;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class InsuranceManagementProductListItemResponse {
    private final UUID insuranceProductId;
    private final String insuranceProductName;
    private final String insuranceProductStatus;
    private final UUID insuranceCompanyId;
    private final String insuranceCompanyName;
    private final UUID insuranceCategoryId;
    private final String insuranceCategoryName;
    private final LocalDate insuranceStartDate;
    private final LocalDate insuranceEndDate;
    private final Integer coveragePeriodYears;
    private final Integer coverageAgeLimit;

    public static InsuranceManagementProductListItemResponse from(InsuranceProduct insuranceProduct) {
        return InsuranceManagementProductListItemResponse.builder()
                .insuranceProductId(insuranceProduct.getId())
                .insuranceProductName(insuranceProduct.getInsuranceProductName())
                .insuranceProductStatus(insuranceProduct.getInsuranceProductStatus())
                .insuranceCompanyId(insuranceProduct.getInsuranceCompany().getId())
                .insuranceCompanyName(insuranceProduct.getInsuranceCompany().getInsuranceCompanyName())
                .insuranceCategoryId(insuranceProduct.getInsuranceCategory().getId())
                .insuranceCategoryName(insuranceProduct.getInsuranceCategory().getInsuranceCategoryName())
                .insuranceStartDate(insuranceProduct.getInsuranceStartDate())
                .insuranceEndDate(insuranceProduct.getInsuranceEndDate())
                .coveragePeriodYears(insuranceProduct.getCoveragePeriodYears())
                .coverageAgeLimit(insuranceProduct.getCoverageAgeLimit())
                .build();
    }
}
