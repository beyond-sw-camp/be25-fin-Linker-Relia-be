package com.linker.relia.commission.dto;

import com.linker.relia.organization.domain.Organization;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class InsuranceCompanyCommissionSummaryResponse {
    private final boolean hasData;
    private final String scope;
    private final String metricType;
    private final String closingMonth;
    private final UUID organizationId;
    private final String organizationCode;
    private final String organizationName;
    private final List<InsuranceCompanyItem> items;

    public static InsuranceCompanyCommissionSummaryResponse fpOf(String closingMonth,
                                                                 List<InsuranceCompanyCommissionSummaryQueryResult> queryResults) {
        return InsuranceCompanyCommissionSummaryResponse.builder()
                .hasData(!queryResults.isEmpty())
                .scope("FP")
                .metricType("NET_COMMISSION")
                .closingMonth(closingMonth)
                .organizationId(null)
                .organizationCode(null)
                .organizationName(null)
                .items(toItems(queryResults))
                .build();
    }

    public static InsuranceCompanyCommissionSummaryResponse branchOf(String closingMonth,
                                                                     Organization organization,
                                                                     List<InsuranceCompanyCommissionSummaryQueryResult> queryResults) {
        return InsuranceCompanyCommissionSummaryResponse.builder()
                .hasData(!queryResults.isEmpty())
                .scope("BRANCH")
                .metricType("NET_INCOME_COMMISSION")
                .closingMonth(closingMonth)
                .organizationId(organization.getId())
                .organizationCode(organization.getOrganizationCode())
                .organizationName(organization.getOrganizationName())
                .items(toItems(queryResults))
                .build();
    }

    public static InsuranceCompanyCommissionSummaryResponse hqOf(String closingMonth,
                                                                 List<InsuranceCompanyCommissionSummaryQueryResult> queryResults) {
        return InsuranceCompanyCommissionSummaryResponse.builder()
                .hasData(!queryResults.isEmpty())
                .scope("HQ")
                .metricType("NET_INCOME_COMMISSION")
                .closingMonth(closingMonth)
                .organizationId(null)
                .organizationCode(null)
                .organizationName(null)
                .items(toItems(queryResults))
                .build();
    }

    private static List<InsuranceCompanyItem> toItems(List<InsuranceCompanyCommissionSummaryQueryResult> queryResults) {
        BigDecimal totalPaymentAmount = queryResults.stream()
                .map(InsuranceCompanyCommissionSummaryQueryResult::getTotalPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return queryResults.stream()
                .map(queryResult -> InsuranceCompanyItem.builder()
                        .insuranceCompanyId(queryResult.getInsuranceCompanyId())
                        .insuranceCompanyName(queryResult.getInsuranceCompanyName())
                        .totalInitialAmount(queryResult.getTotalInitialAmount())
                        .totalMaintenanceAmount(queryResult.getTotalMaintenanceAmount())
                        .totalRecoveryAmount(queryResult.getTotalRecoveryAmount())
                        .totalPaymentAmount(queryResult.getTotalPaymentAmount())
                        .netAmount(queryResult.getNetAmount())
                        .contractCount(queryResult.getContractCount())
                        .ratio(toRatio(queryResult.getTotalPaymentAmount(), totalPaymentAmount))
                        .build())
                .toList();
    }

    private static BigDecimal toRatio(BigDecimal amount, BigDecimal totalAmount) {
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return amount.divide(totalAmount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Getter
    @Builder
    public static class InsuranceCompanyItem {
        private final UUID insuranceCompanyId;
        private final String insuranceCompanyName;
        private final BigDecimal totalInitialAmount;
        private final BigDecimal totalMaintenanceAmount;
        private final BigDecimal totalRecoveryAmount;
        private final BigDecimal totalPaymentAmount;
        private final BigDecimal netAmount;
        private final long contractCount;
        private final BigDecimal ratio;
    }
}
