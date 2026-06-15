package com.linker.relia.commission.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class FpCommissionMonthlyTrendQueryResult {
    private final String closingMonth;
    private final BigDecimal initialCommissionAmount;
    private final BigDecimal maintenanceCommissionAmount;
    private final BigDecimal recoveryAmount;
    private final BigDecimal totalPaidCommissionAmount;
    private final BigDecimal netCommissionAmount;
    private final long contractCount;
    private final long recoveryContractCount;
}
