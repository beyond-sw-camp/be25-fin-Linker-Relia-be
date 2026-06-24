package com.linker.relia.commission.dto;

import com.linker.relia.commission.domain.BranchIncomeCommissionMonthlyClosing;
import com.linker.relia.commission.domain.IncomeCommissionMonthlyClosing;
import com.linker.relia.organization.domain.Organization;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Getter
@Builder
public class OrganizationCommissionSummaryResponse {
    private final boolean hasData;
    private final String scope;
    private final String closingMonth;
    private final UUID organizationId;
    private final String organizationName;
    private final BranchSummary branchSummary;
    private final HqSummary hqSummary;
    private final Comparison comparison;

    public static OrganizationCommissionSummaryResponse branchOf(BranchIncomeCommissionMonthlyClosing current,
                                                                 BranchIncomeCommissionMonthlyClosing previous) {
        Organization organization = current.getOrganization();
        return OrganizationCommissionSummaryResponse.builder()
                .hasData(true)
                .scope("BRANCH")
                .closingMonth(current.getClosingMonth())
                .organizationId(organization.getId())
                .organizationName(organization.getOrganizationName())
                .branchSummary(BranchSummary.builder()
                        .netIncomeCommissionAmount(current.getNetIncomeCommissionAmount())
                        .totalInitialGrossCommissionAmount(current.getTotalInitialGrossCommissionAmount())
                        .totalMaintenanceGrossCommissionAmount(current.getTotalMaintenanceGrossCommissionAmount())
                        .totalPaymentCommissionAmount(current.getTotalPaymentCommissionAmount())
                        .totalInsuranceRecoveryAmount(current.getTotalInsuranceRecoveryAmount())
                        .totalFpRecoveryCollectionAmount(current.getTotalFpRecoveryCollectionAmount())
                        .netRecoveryLossAmount(current.getTotalInsuranceRecoveryAmount().subtract(current.getTotalFpRecoveryCollectionAmount()))
                        .build())
                .hqSummary(null)
                .comparison(toBranchComparison(current, previous))
                .build();
    }

    public static OrganizationCommissionSummaryResponse emptyBranch(String closingMonth, Organization organization) {
        return OrganizationCommissionSummaryResponse.builder()
                .hasData(false)
                .scope("BRANCH")
                .closingMonth(closingMonth)
                .organizationId(organization == null ? null : organization.getId())
                .organizationName(organization == null ? null : organization.getOrganizationName())
                .branchSummary(BranchSummary.builder()
                        .netIncomeCommissionAmount(BigDecimal.ZERO)
                        .totalInitialGrossCommissionAmount(BigDecimal.ZERO)
                        .totalMaintenanceGrossCommissionAmount(BigDecimal.ZERO)
                        .totalPaymentCommissionAmount(BigDecimal.ZERO)
                        .totalInsuranceRecoveryAmount(BigDecimal.ZERO)
                        .totalFpRecoveryCollectionAmount(BigDecimal.ZERO)
                        .netRecoveryLossAmount(BigDecimal.ZERO)
                        .build())
                .hqSummary(null)
                .comparison(null)
                .build();
    }

    public static OrganizationCommissionSummaryResponse hqOf(IncomeCommissionMonthlyClosing current,
                                                             IncomeCommissionMonthlyClosing previous) {
        return OrganizationCommissionSummaryResponse.builder()
                .hasData(true)
                .scope("HQ")
                .closingMonth(current.getClosingMonth())
                .organizationId(null)
                .organizationName(null)
                .branchSummary(null)
                .hqSummary(HqSummary.builder()
                        .netIncomeCommissionAmount(current.getNetIncomeCommissionAmount())
                        .totalInitialGrossCommissionAmount(current.getTotalInitialGrossCommissionAmount())
                        .totalMaintenanceGrossCommissionAmount(current.getTotalMaintenanceGrossCommissionAmount())
                        .totalPaymentCommissionAmount(current.getTotalPaymentCommissionAmount())
                        .totalInsuranceRecoveryAmount(current.getTotalInsuranceRecoveryAmount())
                        .totalFpRecoveryCollectionAmount(current.getTotalFpRecoveryCollectionAmount())
                        .netRecoveryLossAmount(current.getTotalInsuranceRecoveryAmount().subtract(current.getTotalFpRecoveryCollectionAmount()))
                        .build())
                .comparison(toHqComparison(current, previous))
                .build();
    }

    public static OrganizationCommissionSummaryResponse emptyHq(String closingMonth) {
        return OrganizationCommissionSummaryResponse.builder()
                .hasData(false)
                .scope("HQ")
                .closingMonth(closingMonth)
                .organizationId(null)
                .organizationName(null)
                .branchSummary(null)
                .hqSummary(HqSummary.builder()
                        .netIncomeCommissionAmount(BigDecimal.ZERO)
                        .totalInitialGrossCommissionAmount(BigDecimal.ZERO)
                        .totalMaintenanceGrossCommissionAmount(BigDecimal.ZERO)
                        .totalPaymentCommissionAmount(BigDecimal.ZERO)
                        .totalInsuranceRecoveryAmount(BigDecimal.ZERO)
                        .totalFpRecoveryCollectionAmount(BigDecimal.ZERO)
                        .netRecoveryLossAmount(BigDecimal.ZERO)
                        .build())
                .comparison(null)
                .build();
    }

    private static Comparison toBranchComparison(BranchIncomeCommissionMonthlyClosing current,
                                                 BranchIncomeCommissionMonthlyClosing previous) {
        if (previous == null) {
            return null;
        }
        return toComparison(
                previous.getClosingMonth(),
                previous.getNetIncomeCommissionAmount(),
                current.getNetIncomeCommissionAmount()
        );
    }

    private static Comparison toHqComparison(IncomeCommissionMonthlyClosing current,
                                             IncomeCommissionMonthlyClosing previous) {
        if (previous == null) {
            return null;
        }
        return toComparison(
                previous.getClosingMonth(),
                previous.getNetIncomeCommissionAmount(),
                current.getNetIncomeCommissionAmount()
        );
    }

    private static Comparison toComparison(String previousClosingMonth,
                                           BigDecimal previousAmount,
                                           BigDecimal currentAmount) {
        BigDecimal differenceAmount = currentAmount.subtract(previousAmount);
        BigDecimal growthRate = null;
        if (previousAmount.compareTo(BigDecimal.ZERO) != 0) {
            growthRate = differenceAmount
                    .divide(previousAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return Comparison.builder()
                .previousClosingMonth(previousClosingMonth)
                .previousMetricAmount(previousAmount)
                .differenceAmount(differenceAmount)
                .growthRate(growthRate)
                .build();
    }

    @Getter
    @Builder
    public static class BranchSummary {
        private final BigDecimal netIncomeCommissionAmount;
        private final BigDecimal totalInitialGrossCommissionAmount;
        private final BigDecimal totalMaintenanceGrossCommissionAmount;
        private final BigDecimal totalPaymentCommissionAmount;
        private final BigDecimal totalInsuranceRecoveryAmount;
        private final BigDecimal totalFpRecoveryCollectionAmount;
        private final BigDecimal netRecoveryLossAmount;
    }

    @Getter
    @Builder
    public static class HqSummary {
        private final BigDecimal netIncomeCommissionAmount;
        private final BigDecimal totalInitialGrossCommissionAmount;
        private final BigDecimal totalMaintenanceGrossCommissionAmount;
        private final BigDecimal totalPaymentCommissionAmount;
        private final BigDecimal totalInsuranceRecoveryAmount;
        private final BigDecimal totalFpRecoveryCollectionAmount;
        private final BigDecimal netRecoveryLossAmount;
    }

    @Getter
    @Builder
    public static class Comparison {
        private final String previousClosingMonth;
        private final BigDecimal previousMetricAmount;
        private final BigDecimal differenceAmount;
        private final BigDecimal growthRate;
    }
}
