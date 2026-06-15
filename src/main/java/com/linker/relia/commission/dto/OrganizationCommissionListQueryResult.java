package com.linker.relia.commission.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OrganizationCommissionListQueryResult {
    private final UUID organizationId;
    private final String organizationName;
    private final BigDecimal initialCommissionAmount;
    private final BigDecimal maintenanceCommissionAmount;
    private final BigDecimal recoveryAmount;
    private final BigDecimal totalPaymentCommissionAmount;
    private final BigDecimal netCommissionAmount;
    private final int fpCount;
    private final int contractCount;
    private final int recoveryContractCount;
}
