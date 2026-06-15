package com.linker.relia.commission.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class FpCommissionListQueryResult {
    private final UUID fpId;
    private final String fpName;
    private final BigDecimal initialCommissionAmount;
    private final BigDecimal maintenanceCommissionAmount;
    private final BigDecimal recoveryAmount;
    private final BigDecimal totalPaymentCommissionAmount;
    private final BigDecimal netCommissionAmount;
    private final long contractCount;
    private final long recoveryContractCount;
}
