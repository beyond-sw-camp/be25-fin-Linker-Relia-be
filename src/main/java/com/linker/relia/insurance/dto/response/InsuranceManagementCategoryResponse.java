package com.linker.relia.insurance.dto.response;

import com.linker.relia.insurance.domain.InsuranceCategory;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class InsuranceManagementCategoryResponse {
    private final UUID insuranceCategoryId;
    private final String insuranceCategoryCode;
    private final String insuranceCategoryName;
    private final String insuranceCategoryStatus;

    public static InsuranceManagementCategoryResponse from(InsuranceCategory insuranceCategory) {
        return InsuranceManagementCategoryResponse.builder()
                .insuranceCategoryId(insuranceCategory.getId())
                .insuranceCategoryCode(insuranceCategory.getInsuranceCategoryCode())
                .insuranceCategoryName(insuranceCategory.getInsuranceCategoryName())
                .insuranceCategoryStatus(insuranceCategory.getInsuranceCategoryStatus())
                .build();
    }
}
