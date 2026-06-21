package com.linker.relia.insurance.dto.request;

import com.linker.relia.common.dto.request.PageQueryRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InsuranceManagementCompanyListRequest extends PageQueryRequest {
    private String insuranceCompanyName;

    public String normalizedInsuranceCompanyName() {
        if (insuranceCompanyName == null) {
            return null;
        }

        String trimmed = insuranceCompanyName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
