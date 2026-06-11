package com.linker.relia.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class ContractCreateRequest {
    @NotNull(message = "고객 ID는 필수입니다.")
    private UUID customerId;

    @NotNull(message = "보험상품 ID는 필수입니다.")
    private UUID insuranceProductId;

    @NotNull(message = "계약 체결일은 필수입니다.")
    private LocalDate contractDate;

    @NotNull(message = "계약 시작일은 필수입니다.")
    private LocalDate contractStartDate;

    @NotNull(message = "만기일은 필수입니다.")
    private LocalDate contractEndDate;

    private LocalDate coverageStartDate;

    private LocalDate coverageEndDate;

    @NotNull(message = "납입 기간은 필수입니다.")
    @Positive(message = "납입 기간은 1년 이상이어야 합니다.")
    private Integer paymentPeriodYears;

    @NotBlank(message = "납입 주기는 필수입니다.")
    @Pattern(regexp = "MONTHLY", message = "납입 주기는 MONTHLY만 지원합니다.")
    private String paymentCycle;

    @NotNull(message = "월 보험료는 필수입니다.")
    @Positive(message = "월 보험료는 0보다 커야 합니다.")
    private BigDecimal monthlyPremium;

    private String coverageSummary;
}
