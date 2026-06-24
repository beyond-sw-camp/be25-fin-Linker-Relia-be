package com.linker.relia.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerContractSummaryResponse {
    private long totalContractCount;
    private BigDecimal totalMonthlyPremium;
    private Map<String, Long> contractStatusCounts;

    public static CustomerContractSummaryResponse empty() {
        return CustomerContractSummaryResponse.builder()
                .totalContractCount(0L)
                .totalMonthlyPremium(BigDecimal.ZERO)
                .contractStatusCounts(defaultContractStatusCounts())
                .build();
    }

    public static Map<String, Long> defaultContractStatusCounts() {
        Map<String, Long> contractStatusCounts = new LinkedHashMap<>();
        contractStatusCounts.put("MAINTENANCE", 0L);
        contractStatusCounts.put("COMPLETED", 0L);
        contractStatusCounts.put("TERMINATED", 0L);
        contractStatusCounts.put("LAPSED", 0L);
        return contractStatusCounts;
    }
}
