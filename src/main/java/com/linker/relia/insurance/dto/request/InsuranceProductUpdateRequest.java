package com.linker.relia.insurance.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class InsuranceProductUpdateRequest {
    @NotBlank(message = "보험상품 상태는 필수입니다.")
    private String insuranceProductStatus;

    private String productDescription;

    public String normalizedInsuranceProductStatus() {
        return insuranceProductStatus == null ? null : insuranceProductStatus.trim();
    }

    public String normalizedProductDescription() {
        if (productDescription == null) {
            return null;
        }

        String trimmed = productDescription.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
