package com.linker.relia.dashboard.dto;

import com.linker.relia.common.dto.request.PageQueryRequest;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardFpRankingRequest extends PageQueryRequest {

    @Pattern(
            regexp = "^\\d{4}-(0[1-9]|1[0-2])$",
            message = "closingMonth는 YYYY-MM 형식이어야 합니다."
    )
    private String closingMonth;

    private String organizationCode;

    private DashboardFpRankOrder rankOrder = DashboardFpRankOrder.TOP;
}
