package com.linker.relia.insurance.dto.response;

import com.linker.relia.insurance.domain.InsuranceProduct;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class InsuranceProductDetailResponse {
    private final UUID insuranceProductId;
    private final String insuranceProductCode;
    private final String insuranceProductName;
    private final String insuranceProductStatus;
    private final UUID insuranceCompanyId;
    private final String insuranceCompanyName;
    private final UUID insuranceCategoryId;
    private final String insuranceCategoryName;
    private final LocalDate insuranceStartDate;
    private final LocalDate insuranceEndDate;
    private final String coveragePeriodType;
    private final Integer coveragePeriodYears;
    private final Integer coverageAgeLimit;
    private final Boolean isLifetimeCoverage;
    private final Boolean isRenewable;
    private final Integer renewalCycle;
    private final String productDescription;

    public static InsuranceProductDetailResponse from(InsuranceProduct insuranceProduct) {
        return InsuranceProductDetailResponse.builder()
                .insuranceProductId(insuranceProduct.getId())
                .insuranceProductCode(insuranceProduct.getInsuranceProductCode())
                .insuranceProductName(insuranceProduct.getInsuranceProductName())
                .insuranceProductStatus(insuranceProduct.getInsuranceProductStatus())
                .insuranceCompanyId(insuranceProduct.getInsuranceCompany().getId())
                .insuranceCompanyName(insuranceProduct.getInsuranceCompany().getInsuranceCompanyName())
                .insuranceCategoryId(insuranceProduct.getInsuranceCategory().getId())
                .insuranceCategoryName(insuranceProduct.getInsuranceCategory().getInsuranceCategoryName())
                .insuranceStartDate(insuranceProduct.getInsuranceStartDate())
                .insuranceEndDate(insuranceProduct.getInsuranceEndDate())
                .coveragePeriodType(insuranceProduct.getCoveragePeriodType())
                .coveragePeriodYears(insuranceProduct.getCoveragePeriodYears())
                .coverageAgeLimit(insuranceProduct.getCoverageAgeLimit())
                .isLifetimeCoverage(insuranceProduct.getIsLifetimeCoverage())
                .isRenewable(insuranceProduct.getIsRenewable())
                .renewalCycle(insuranceProduct.getRenewalCycle())
                .productDescription(insuranceProduct.getProductDescription())
                .build();
    }
}
