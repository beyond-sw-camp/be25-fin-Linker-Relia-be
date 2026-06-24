package com.linker.relia.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
public class FpDetailResponse {
    private final UUID fpId;
    private final String empCode;
    private final String fpName;
    private final UUID organizationId;
    private final String organizationName;
    private final String phone;
    private final String email;
    private final LocalDate hireDate;
    private final PerformanceSummary performanceSummary;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class PerformanceSummary {
        private final String closingMonth;
        private final int completedContractCount;
        private final int newContractCount;
        private final BigDecimal retentionRate;
        private final int totalRank;
        private final int branchRank;
    }
}
