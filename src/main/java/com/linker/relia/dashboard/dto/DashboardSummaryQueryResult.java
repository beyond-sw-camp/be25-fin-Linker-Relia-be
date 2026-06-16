package com.linker.relia.dashboard.dto;

import java.math.BigDecimal;

public record DashboardSummaryQueryResult(
        long currentNewContractCount,
        long previousNewContractCount,
        BigDecimal currentRetentionRate,
        BigDecimal previousRetentionRate,
        Integer currentBranchRank,
        Integer previousBranchRank,
        long currentCustomerCount,
        long customerNetIncreaseCount,
        long currentNewHandoverCount,
        long previousNewHandoverCount,
        BigDecimal currentExpectedCommissionAmount,
        BigDecimal previousClosedCommissionAmount
) {
}
