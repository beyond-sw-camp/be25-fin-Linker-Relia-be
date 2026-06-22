package com.linker.relia.insurance.dto.request;

import com.linker.relia.common.dto.request.PageQueryRequest;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InsuranceManagementCompanyListRequest extends PageQueryRequest {
    private String insuranceCompanyName;

    @Pattern(regexp = "ALL|ACTIVE|INACTIVE", message = "보험사 상태는 ALL, ACTIVE 또는 INACTIVE 여야 합니다.")
    private String insuranceCompanyStatus;

    public String normalizedInsuranceCompanyName() {
        if (insuranceCompanyName == null) {
            return null;
        }

        String trimmed = insuranceCompanyName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String normalizedInsuranceCompanyStatus() {
        if (insuranceCompanyStatus == null) {
            return null;
        }

        String trimmed = insuranceCompanyStatus.trim();
        if (trimmed.isEmpty() || "ALL".equals(trimmed)) {
            return null;
        }

        return trimmed;
    }
}
