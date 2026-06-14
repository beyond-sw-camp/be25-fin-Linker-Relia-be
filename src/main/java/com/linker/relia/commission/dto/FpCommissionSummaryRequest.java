package com.linker.relia.commission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FpCommissionSummaryRequest {
    @NotBlank(message = "closingMonth is required.")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "closingMonth must be in YYYY-MM format.")
    private String closingMonth;
}
