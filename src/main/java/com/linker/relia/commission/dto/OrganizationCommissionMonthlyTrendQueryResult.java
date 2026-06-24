package com.linker.relia.commission.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OrganizationCommissionMonthlyTrendQueryResult {
    private final String scope;
    private final String closingMonth;
    private final UUID organizationId;
    private final String organizationName;
    private final BigDecimal initialCommissionAmount;
    private final BigDecimal maintenanceCommissionAmount;
    private final BigDecimal recoveryAmount;
    private final BigDecimal totalCommissionAmount;
    private final BigDecimal netCommissionAmount;
    private final Long fpCount;
    private final Long contractCount;
    private final Long recoveryContractCount;
}
