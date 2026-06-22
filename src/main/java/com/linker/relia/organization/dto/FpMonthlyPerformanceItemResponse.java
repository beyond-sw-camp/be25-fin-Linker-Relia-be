package com.linker.relia.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@Builder
public class FpMonthlyPerformanceItemResponse {
    private final String closingMonth;
    private final int completedContractCount;
    private final int newContractCount;
    private final BigDecimal retentionRate;
    private final int totalRank;
    private final int branchRank;
}
