package com.linker.relia.commission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommissionPaymentTypeSummaryRequest {
    @NotBlank(message = "조회할 마감 월은 필수입니다.")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "마감 월은 yyyy-MM 형식이어야 합니다.")
    private String closingMonth;

    private String organizationCode;
}
