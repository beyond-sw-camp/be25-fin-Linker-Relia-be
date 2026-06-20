package com.linker.relia.dashboard.dto;

import java.math.BigDecimal;

public record DashboardKpiQueryResult(
        long fpCount,
        long customerCount,
        long interestCustomerCount,
        long totalContractCount,
        BigDecimal contractSuccessRate,
        BigDecimal retentionRate,
        long terminatedContractCount,
        BigDecimal netIncomeCommissionAmount,
        BigDecimal totalPaymentCommissionAmount
) {
}
