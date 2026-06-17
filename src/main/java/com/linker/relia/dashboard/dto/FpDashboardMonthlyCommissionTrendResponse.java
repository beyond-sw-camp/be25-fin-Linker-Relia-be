package com.linker.relia.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class FpDashboardMonthlyCommissionTrendResponse {
    private final LocalDate referenceDate;
    private final String startMonth;
    private final String endMonth;
    private final List<MonthlyCommissionTrendItem> monthlyTrends;

    @Getter
    @Builder
    public static class MonthlyCommissionTrendItem {
        private final String month;
        private final BigDecimal netCommissionAmount;
    }
}
