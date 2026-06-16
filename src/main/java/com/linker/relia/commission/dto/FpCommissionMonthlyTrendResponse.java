package com.linker.relia.commission.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class FpCommissionMonthlyTrendResponse {
    private final String closingMonth;
    private final BigDecimal initialCommissionAmount;
    private final BigDecimal maintenanceCommissionAmount;
    private final BigDecimal recoveryAmount;
    private final BigDecimal totalPaidCommissionAmount;
    private final BigDecimal netCommissionAmount;
    private final long contractCount;
    private final long recoveryContractCount;

    public static FpCommissionMonthlyTrendResponse from(FpCommissionMonthlyTrendQueryResult result) {
        return FpCommissionMonthlyTrendResponse.builder()
                .closingMonth(result.getClosingMonth())
                .initialCommissionAmount(result.getInitialCommissionAmount())
                .maintenanceCommissionAmount(result.getMaintenanceCommissionAmount())
                .recoveryAmount(result.getRecoveryAmount())
                .totalPaidCommissionAmount(result.getTotalPaidCommissionAmount())
                .netCommissionAmount(result.getNetCommissionAmount())
                .contractCount(result.getContractCount())
                .recoveryContractCount(result.getRecoveryContractCount())
                .build();
    }

    public static FpCommissionMonthlyTrendResponse empty(String closingMonth) {
        return FpCommissionMonthlyTrendResponse.builder()
                .closingMonth(closingMonth)
                .initialCommissionAmount(BigDecimal.ZERO)
                .maintenanceCommissionAmount(BigDecimal.ZERO)
                .recoveryAmount(BigDecimal.ZERO)
                .totalPaidCommissionAmount(BigDecimal.ZERO)
                .netCommissionAmount(BigDecimal.ZERO)
                .contractCount(0L)
                .recoveryContractCount(0L)
                .build();
    }
}
