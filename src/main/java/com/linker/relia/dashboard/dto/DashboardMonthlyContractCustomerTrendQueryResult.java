package com.linker.relia.dashboard.dto;

public record DashboardMonthlyContractCustomerTrendQueryResult(
        String closingMonth,
        long newContractCount,
        long customerCount
) {
}
