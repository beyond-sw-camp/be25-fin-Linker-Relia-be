package com.linker.relia.dashboard.dto;

import java.math.BigDecimal;

public record DashboardMonthlyCommissionTrendQueryResult(
        String closingMonth,
        BigDecimal netCommissionAmount
) {
}
