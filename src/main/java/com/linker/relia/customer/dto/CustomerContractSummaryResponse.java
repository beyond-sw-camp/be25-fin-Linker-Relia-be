package com.linker.relia.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerContractSummaryResponse {
    private long totalContractCount;
    private BigDecimal totalMonthlyPremium;
    private long activeContractCount;
    private long maturityDueContractCount;

    public static CustomerContractSummaryResponse empty() {
        return CustomerContractSummaryResponse.builder()
                .totalContractCount(0L)
                .totalMonthlyPremium(BigDecimal.ZERO)
                .activeContractCount(0L)
                .maturityDueContractCount(0L)
                .build();
    }
}
