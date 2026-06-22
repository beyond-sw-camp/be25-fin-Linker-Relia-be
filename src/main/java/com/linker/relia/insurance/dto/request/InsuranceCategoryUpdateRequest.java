package com.linker.relia.insurance.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class InsuranceCategoryUpdateRequest {
    private String insuranceCategoryName;

    @Pattern(regexp = "ACTIVE|INACTIVE", message = "보종 상태는 ACTIVE 또는 INACTIVE 여야 합니다.")
    private String insuranceCategoryStatus;

    public String normalizedInsuranceCategoryName() {
        if (insuranceCategoryName == null) {
            return null;
        }

        String trimmed = insuranceCategoryName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String normalizedInsuranceCategoryStatus() {
        if (insuranceCategoryStatus == null) {
            return null;
        }

        String trimmed = insuranceCategoryStatus.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
