package com.linker.relia.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class OrganizationDashboardKpiResponse {
    private final String closingMonth;
    private final String comparisonClosingMonth;
    private final String organizationCode;
    private final String organizationName;
    private final long fpCount;
    private final long fpCountDiff;
    private final long customerCount;
    private final long customerCountDiff;
    private final long totalContractCount;
    private final long totalContractCountDiff;
    private final long interestCustomerCount;
    private final long interestCustomerCountDiff;
    private final BigDecimal interestCustomerRate;
    private final BigDecimal contractSuccessRate;
    private final BigDecimal contractSuccessRateDiff;
    private final BigDecimal retentionRate;
    private final BigDecimal retentionRateDiff;
    private final long terminatedContractCount;
    private final long terminatedContractCountDiff;
    private final BigDecimal netIncomeCommissionAmount;
    private final BigDecimal netIncomeCommissionDiffAmount;
    private final BigDecimal totalPaymentCommissionAmount;
    private final BigDecimal totalPaymentCommissionDiffAmount;
}
