package com.linker.relia.commission.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class InsuranceCompanyCommissionSummaryQueryResult {
    private final UUID insuranceCompanyId;
    private final String insuranceCompanyName;
    private final BigDecimal totalInitialAmount;
    private final BigDecimal totalMaintenanceAmount;
    private final BigDecimal totalRecoveryAmount;
    private final BigDecimal totalPaymentAmount;
    private final BigDecimal netAmount;
    private final long contractCount;
}
