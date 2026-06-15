package com.linker.relia.commission.dto;

import com.linker.relia.commission.domain.FpCommissionMonthlyClosing;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Builder
public class FpCommissionSummaryResponse {
    private final boolean hasData;
    private final String closingMonth;
    private final BigDecimal totalInitialPaymentAmount;
    private final BigDecimal totalMaintenancePaymentAmount;
    private final BigDecimal totalRecoveryCollectionAmount;
    private final BigDecimal totalPaymentAmount;
    private final BigDecimal netCommissionAmount;
    private final long contractCount;
    private final long recoveryContractCount;
    private final Comparison comparison;

    public static FpCommissionSummaryResponse of(FpCommissionMonthlyClosing current,
                                                 FpCommissionMonthlyClosing previous) {
        return FpCommissionSummaryResponse.builder()
                .hasData(true)
                .closingMonth(current.getClosingMonth())
                .totalInitialPaymentAmount(current.getTotalInitialPaymentAmount())
                .totalMaintenancePaymentAmount(current.getTotalMaintenancePaymentAmount())
                .totalRecoveryCollectionAmount(current.getTotalRecoveryCollectionAmount())
                .totalPaymentAmount(current.getTotalPaymentAmount())
                .netCommissionAmount(current.getNetCommissionAmount())
                .contractCount(current.getContractCount())
                .recoveryContractCount(current.getRecoveryContractCount())
                .comparison(toComparison(current, previous))
                .build();
    }

    public static FpCommissionSummaryResponse empty(String closingMonth) {
        return FpCommissionSummaryResponse.builder()
                .hasData(false)
                .closingMonth(closingMonth)
                .totalInitialPaymentAmount(BigDecimal.ZERO)
                .totalMaintenancePaymentAmount(BigDecimal.ZERO)
                .totalRecoveryCollectionAmount(BigDecimal.ZERO)
                .totalPaymentAmount(BigDecimal.ZERO)
                .netCommissionAmount(BigDecimal.ZERO)
                .contractCount(0L)
                .recoveryContractCount(0L)
                .comparison(null)
                .build();
    }

    private static Comparison toComparison(FpCommissionMonthlyClosing current, FpCommissionMonthlyClosing previous) {
        if (previous == null) {
            return null;
        }

        BigDecimal previousNetCommissionAmount = previous.getNetCommissionAmount();
        BigDecimal differenceAmount = current.getNetCommissionAmount().subtract(previousNetCommissionAmount);
        BigDecimal growthRate = null;
        if (previousNetCommissionAmount.compareTo(BigDecimal.ZERO) != 0) {
            growthRate = differenceAmount
                    .divide(previousNetCommissionAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return Comparison.builder()
                .previousClosingMonth(previous.getClosingMonth())
                .previousNetCommissionAmount(previousNetCommissionAmount)
                .differenceAmount(differenceAmount)
                .growthRate(growthRate)
                .build();
    }

    @Getter
    @Builder
    public static class Comparison {
        private final String previousClosingMonth;
        private final BigDecimal previousNetCommissionAmount;
        private final BigDecimal differenceAmount;
        private final BigDecimal growthRate;
    }
}
