package com.linker.relia.consultation.dto.request;

import com.linker.relia.customer.domain.CustomerMaritalStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class CustomerInfoRequest {

    @NotBlank(message = "고객명은 필수입니다.")
    private String customerName;

    @NotBlank(message = "성별은 필수입니다.")
    private String customerGender;

    @NotNull(message = "생년월일은 필수입니다.")
    private LocalDate customerBirthDate;

    @NotBlank(message = "휴대폰 번호는 필수입니다.")
    private String customerPhone;

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @NotBlank(message = "이메일은 필수입니다.")
    private String customerEmail;

    @NotBlank(message = "우편번호는 필수입니다.")
    private String customerZipcode;

    @NotBlank(message = "도로명 주소는 필수입니다.")
    private String customerAddressRoad;

    private String customerAddressDetail;

    @NotBlank(message = "직업은 필수입니다.")
    private String customerJob;

    @NotBlank(message = "직장명은 필수입니다.")
    private String customerCompanyName;

    @NotNull(message = "연소득은 필수입니다.")
    private BigDecimal customerAnnualIncome;

    @NotNull(message = "자산 규모는 필수입니다.")
    private BigDecimal customerAssetSize;

    @NotBlank(message = "부채 현황은 필수입니다.")
    private String customerDebtStatus;

    @NotNull(message = "흡연 여부는 필수입니다.")
    private Boolean customerIsSmoker;

    @NotNull(message = "음주 여부는 필수입니다.")
    private Boolean customerIsDrinker;

    @NotNull(message = "결혼 여부는 필수입니다.")
    private CustomerMaritalStatus customerMaritalStatus;

    @NotNull(message = "부양가족 수는 필수입니다.")
    private Integer customerDependentsCount;

    private List<String> underlyingDiseaseCodes;
}
