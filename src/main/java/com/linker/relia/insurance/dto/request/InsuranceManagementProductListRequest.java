package com.linker.relia.insurance.dto.request;

import com.linker.relia.common.dto.request.PageQueryRequest;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class InsuranceManagementProductListRequest extends PageQueryRequest {
    private UUID insuranceCompanyId;
    private UUID insuranceCategoryId;
    private String insuranceProductName;

    @Pattern(regexp = "ALL|ON_SALE|SALE_ENDED", message = "판매 상태는 ALL, ON_SALE 또는 SALE_ENDED 여야 합니다.")
    private String saleStatus;

    public String normalizedInsuranceProductName() {
        if (insuranceProductName == null) {
            return null;
        }

        String trimmed = insuranceProductName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String normalizedSaleStatus() {
        if (saleStatus == null) {
            return null;
        }

        String trimmed = saleStatus.trim();
        if (trimmed.isEmpty() || "ALL".equals(trimmed)) {
            return null;
        }

        return trimmed;
    }
}
