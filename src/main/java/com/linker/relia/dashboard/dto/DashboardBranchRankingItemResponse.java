package com.linker.relia.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class DashboardBranchRankingItemResponse {
    private final int rank;
    private final UUID organizationId;
    private final String organizationCode;
    private final String organizationName;
    private final BigDecimal netIncomeCommissionAmount;
    private final Integer previousRank;
    private final Integer rankChange;
}
