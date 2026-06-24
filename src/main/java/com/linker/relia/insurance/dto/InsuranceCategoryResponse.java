package com.linker.relia.insurance.dto;

import com.linker.relia.insurance.domain.InsuranceCategory;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class InsuranceCategoryResponse {
    private final UUID insuranceCategoryId;
    private final String insuranceCategoryCode;
    private final String insuranceCategoryName;

    public static InsuranceCategoryResponse from(InsuranceCategory insuranceCategory) {
        return InsuranceCategoryResponse.builder()
                .insuranceCategoryId(insuranceCategory.getId())
                .insuranceCategoryCode(insuranceCategory.getInsuranceCategoryCode())
                .insuranceCategoryName(insuranceCategory.getInsuranceCategoryName())
                .build();
    }
}
