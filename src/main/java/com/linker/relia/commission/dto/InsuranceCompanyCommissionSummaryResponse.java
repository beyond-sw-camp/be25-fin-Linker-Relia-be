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
                                                                 List<InsuranceCompanyCommissionSummaryRow> rows) {
        return InsuranceCompanyCommissionSummaryResponse.builder()
                .hasData(!rows.isEmpty())
                .scope("FP")
                .metricType("NET_COMMISSION")
                .closingMonth(closingMonth)
                .organizationId(null)
                .organizationCode(null)
                .organizationName(null)
                .items(toItems(rows))
                .build();
    }

    public static InsuranceCompanyCommissionSummaryResponse branchOf(String closingMonth,
                                                                     Organization organization,
                                                                     List<InsuranceCompanyCommissionSummaryRow> rows) {
        return InsuranceCompanyCommissionSummaryResponse.builder()
                .hasData(!rows.isEmpty())
                .scope("BRANCH")
                .metricType("NET_COMMISSION")
                .closingMonth(closingMonth)
                .organizationId(organization.getId())
                .organizationCode(organization.getOrganizationCode())
                .organizationName(organization.getOrganizationName())
                .items(toItems(rows))
                .build();
    }

    public static InsuranceCompanyCommissionSummaryResponse hqOf(String closingMonth,
                                                                 List<InsuranceCompanyCommissionSummaryRow> rows) {
        return InsuranceCompanyCommissionSummaryResponse.builder()
                .hasData(!rows.isEmpty())
                .scope("HQ")
                .metricType("NET_INCOME_COMMISSION")
                .closingMonth(closingMonth)
                .organizationId(null)
                .organizationCode(null)
                .organizationName(null)
                .items(toItems(rows))
                .build();
    }

    private static List<InsuranceCompanyItem> toItems(List<InsuranceCompanyCommissionSummaryRow> rows) {
        BigDecimal totalPaymentAmount = rows.stream()
                .map(InsuranceCompanyCommissionSummaryRow::getTotalPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return rows.stream()
                .map(row -> InsuranceCompanyItem.builder()
                        .insuranceCompanyId(row.getInsuranceCompanyId())
                        .insuranceCompanyName(row.getInsuranceCompanyName())
                        .totalInitialAmount(row.getTotalInitialAmount())
                        .totalMaintenanceAmount(row.getTotalMaintenanceAmount())
                        .totalRecoveryAmount(row.getTotalRecoveryAmount())
                        .totalPaymentAmount(row.getTotalPaymentAmount())
                        .netAmount(row.getNetAmount())
                        .contractCount(row.getContractCount())
                        .ratio(toRatio(row.getTotalPaymentAmount(), totalPaymentAmount))
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
