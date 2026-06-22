package com.linker.relia.insurance.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class InsuranceCategoryCreateRequest {
    @NotBlank(message = "보종명은 필수입니다.")
    private String insuranceCategoryName;

    public String normalizedInsuranceCategoryName() {
        return insuranceCategoryName == null ? null : insuranceCategoryName.trim();
    }
}
