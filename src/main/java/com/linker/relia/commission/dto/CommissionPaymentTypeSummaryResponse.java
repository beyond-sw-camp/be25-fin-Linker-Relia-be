package com.linker.relia.commission.dto;

import com.linker.relia.commission.domain.BranchCommissionMonthlyClosing;
import com.linker.relia.commission.domain.FpCommissionMonthlyClosing;
import com.linker.relia.commission.domain.IncomeCommissionMonthlyClosing;
import com.linker.relia.organization.domain.Organization;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CommissionPaymentTypeSummaryResponse {
    private final boolean hasData;
    private final String scope;
    private final String closingMonth;
    private final UUID organizationId;
    private final String organizationCode;
    private final String organizationName;
    private final List<PaymentTypeItem> items;

    public static CommissionPaymentTypeSummaryResponse fpOf(FpCommissionMonthlyClosing closing) {
        return CommissionPaymentTypeSummaryResponse.builder()
                .hasData(true)
                .scope("FP")
                .closingMonth(closing.getClosingMonth())
                .organizationId(null)
                .organizationCode(null)
                .organizationName(null)
                .items(items(
                        closing.getTotalInitialPaymentAmount(),
                        closing.getTotalMaintenancePaymentAmount(),
                        closing.getTotalRecoveryCollectionAmount()
                ))
                .build();
    }

    public static CommissionPaymentTypeSummaryResponse emptyFp(String closingMonth) {
        return CommissionPaymentTypeSummaryResponse.builder()
                .hasData(false)
                .scope("FP")
                .closingMonth(closingMonth)
                .organizationId(null)
                .organizationCode(null)
                .organizationName(null)
                .items(items(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .build();
    }

    public static CommissionPaymentTypeSummaryResponse branchOf(BranchCommissionMonthlyClosing closing) {
        Organization organization = closing.getOrganization();
        return CommissionPaymentTypeSummaryResponse.builder()
                .hasData(true)
                .scope("BRANCH")
                .closingMonth(closing.getClosingMonth())
                .organizationId(organization.getId())
                .organizationCode(organization.getOrganizationCode())
                .organizationName(organization.getOrganizationName())
                .items(items(
                        closing.getTotalInitialPaymentAmount(),
                        closing.getTotalMaintenancePaymentAmount(),
                        closing.getTotalRecoveryCollectionAmount()
                ))
                .build();
    }

    public static CommissionPaymentTypeSummaryResponse emptyBranch(String closingMonth, Organization organization) {
        return CommissionPaymentTypeSummaryResponse.builder()
                .hasData(false)
                .scope("BRANCH")
                .closingMonth(closingMonth)
                .organizationId(organization == null ? null : organization.getId())
                .organizationCode(organization == null ? null : organization.getOrganizationCode())
                .organizationName(organization == null ? null : organization.getOrganizationName())
                .items(items(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .build();
    }

    public static CommissionPaymentTypeSummaryResponse hqOf(IncomeCommissionMonthlyClosing closing) {
        return CommissionPaymentTypeSummaryResponse.builder()
                .hasData(true)
                .scope("HQ")
                .closingMonth(closing.getClosingMonth())
                .organizationId(null)
                .organizationCode(null)
                .organizationName(null)
                .items(items(
                        closing.getTotalInitialGrossCommissionAmount(),
                        closing.getTotalMaintenanceGrossCommissionAmount(),
                        closing.getTotalInsuranceRecoveryAmount()
                ))
                .build();
    }

    public static CommissionPaymentTypeSummaryResponse emptyHq(String closingMonth) {
        return CommissionPaymentTypeSummaryResponse.builder()
                .hasData(false)
                .scope("HQ")
                .closingMonth(closingMonth)
                .organizationId(null)
                .organizationCode(null)
                .organizationName(null)
                .items(items(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .build();
    }

    private static List<PaymentTypeItem> items(BigDecimal initialAmount,
                                               BigDecimal maintenanceAmount,
                                               BigDecimal recoveryAmount) {
        BigDecimal total = initialAmount.add(maintenanceAmount).add(recoveryAmount);
        return List.of(
                PaymentTypeItem.of("INITIAL", "초회 수수료", initialAmount, ratio(initialAmount, total)),
                PaymentTypeItem.of("MAINTENANCE", "유지 수수료", maintenanceAmount, ratio(maintenanceAmount, total)),
                PaymentTypeItem.of("RECOVERY", "환수 금액", recoveryAmount, ratio(recoveryAmount, total))
        );
    }

    private static BigDecimal ratio(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return amount.divide(total, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Getter
    @Builder
    public static class PaymentTypeItem {
        private final String paymentType;
        private final String label;
        private final BigDecimal amount;
        private final BigDecimal ratio;

        public static PaymentTypeItem of(String paymentType, String label, BigDecimal amount, BigDecimal ratio) {
            return PaymentTypeItem.builder()
                    .paymentType(paymentType)
                    .label(label)
                    .amount(amount)
                    .ratio(ratio)
                    .build();
        }
    }
}
