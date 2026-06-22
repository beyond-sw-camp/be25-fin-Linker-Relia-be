package com.linker.relia.insurance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InsuranceCompanyCreateRequest {
    @NotBlank(message = "보험사명은 필수입니다.")
    private String insuranceCompanyName;

    @NotBlank(message = "대표 연락처는 필수입니다.")
    @Pattern(regexp = "^[0-9\\-]+$", message = "대표 연락처는 숫자와 하이픈만 입력할 수 있습니다.")
    private String insuranceCompanyPhone;
}
