package com.linker.relia.contract.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ContractMonthlyTrendResponse {
    private final String month;
    private final long contractCount;
    private final BigDecimal totalMonthlyPremiumAmount;
}
