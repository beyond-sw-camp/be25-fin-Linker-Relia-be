package com.linker.relia.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class FpDashboardMonthlyContractCustomerTrendResponse {
    private final LocalDate referenceDate;
    private final String startMonth;
    private final String endMonth;
    private final List<MonthlyContractCustomerTrendItem> monthlyTrends;

    @Getter
    @Builder
    public static class MonthlyContractCustomerTrendItem {
        private final String month;
        private final long newContractCount;
        private final long customerCount;
    }
}
