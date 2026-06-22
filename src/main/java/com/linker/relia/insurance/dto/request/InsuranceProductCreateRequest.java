package com.linker.relia.insurance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
public class InsuranceProductCreateRequest {
    @NotNull(message = "보험사는 필수입니다.")
    private UUID insuranceCompanyId;

    @NotNull(message = "보종은 필수입니다.")
    private UUID insuranceCategoryId;

    @NotBlank(message = "보험상품명은 필수입니다.")
    private String insuranceProductName;

    @NotNull(message = "출시일은 필수입니다.")
    private LocalDate insuranceStartDate;

    private LocalDate insuranceEndDate;

    @NotBlank(message = "보장기간 유형은 필수입니다.")
    @Pattern(regexp = "YEARS|AGE|LIFETIME", message = "보장기간 유형은 YEARS, AGE, LIFETIME 중 하나여야 합니다.")
    private String coveragePeriodType;

    private Integer coveragePeriodYears;

    private Integer coverageAgeLimit;

    @NotNull(message = "종신 보장 여부는 필수입니다.")
    private Boolean isLifetimeCoverage;

    @NotNull(message = "갱신 여부는 필수입니다.")
    private Boolean isRenewable;

    private Integer renewalCycle;

    private String productDescription;

    public String normalizedInsuranceProductName() {
        return insuranceProductName == null ? null : insuranceProductName.trim();
    }

    public String normalizedCoveragePeriodType() {
        return coveragePeriodType == null ? null : coveragePeriodType.trim();
    }

    public String normalizedProductDescription() {
        if (productDescription == null) {
            return null;
        }

        String trimmed = productDescription.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
