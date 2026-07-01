package com.linker.relia.dashboard.dto;

import com.linker.relia.commission.dto.OrganizationScopedClosingMonthRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardInsuranceProductRankingRequest extends OrganizationScopedClosingMonthRequest {
    @Min(value = 1, message = "limit must be at least 1.")
    @Max(value = 100, message = "limit must be 100 or less.")
    private Integer limit = 10;
}
