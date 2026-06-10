package com.linker.relia.contract.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class InsuranceCompanyContractStatusResponse {
    private final String insuranceCompanyName;
    private final long totalContractCount;
    private final BigDecimal totalMonthlyPremiumAmount;
    private final BigDecimal retentionRate;
}
