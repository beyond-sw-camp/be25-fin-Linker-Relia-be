package com.linker.relia.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class FpDashboardSummaryResponse {
    private final LocalDate referenceDate;
    private final String comparisonClosingMonth;
    private final long newContractCount;
    private final long newContractDiff;
    private final BigDecimal retentionRate;
    private final BigDecimal retentionRateDiff;
    private final Integer branchRank;
    private final Integer branchRankChange;
    private final long customerCount;
    private final long customerDiff;
    private final long newHandoverCount;
    private final long handoverDiff;
    private final BigDecimal expectedCommissionAmount;
    private final BigDecimal commissionDiffAmount;
}
