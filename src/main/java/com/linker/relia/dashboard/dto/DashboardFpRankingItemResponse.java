package com.linker.relia.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class DashboardFpRankingItemResponse {
    private final int rank;
    private final UUID fpId;
    private final String empCode;
    private final String fpName;
    private final String organizationCode;
    private final String organizationName;
    private final int newContractCount;
    private final int managedContractCount;
    private final BigDecimal retentionRate;
    private final int customerCount;
    private final BigDecimal commissionAmount;
}
