package com.linker.relia.commission.dto;

import com.linker.relia.common.dto.request.PageQueryRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FpCommissionListRequest extends PageQueryRequest {
    @NotBlank(message = "closingMonth는 필수입니다.")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "closingMonth는 YYYY-MM 형식이어야 합니다.")
    private String closingMonth;

    private String organizationCode;

    public FpCommissionListRequest() {
        setSize(5);
    }
}
