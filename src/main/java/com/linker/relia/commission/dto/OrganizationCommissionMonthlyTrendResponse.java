package com.linker.relia.commission.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class OrganizationCommissionMonthlyTrendResponse {
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

    public static OrganizationCommissionMonthlyTrendResponse from(OrganizationCommissionMonthlyTrendQueryResult result) {
        return OrganizationCommissionMonthlyTrendResponse.builder()
                .scope(result.getScope())
                .closingMonth(result.getClosingMonth())
                .organizationId(result.getOrganizationId())
                .organizationName(result.getOrganizationName())
                .initialCommissionAmount(result.getInitialCommissionAmount())
                .maintenanceCommissionAmount(result.getMaintenanceCommissionAmount())
                .recoveryAmount(result.getRecoveryAmount())
                .totalCommissionAmount(result.getTotalCommissionAmount())
                .netCommissionAmount(result.getNetCommissionAmount())
                .fpCount(result.getFpCount())
                .contractCount(result.getContractCount())
                .recoveryContractCount(result.getRecoveryContractCount())
                .build();
    }

    public static OrganizationCommissionMonthlyTrendResponse emptyBranch(String closingMonth,
                                                                        UUID organizationId,
                                                                        String organizationName) {
        return OrganizationCommissionMonthlyTrendResponse.builder()
                .scope("BRANCH")
                .closingMonth(closingMonth)
                .organizationId(organizationId)
                .organizationName(organizationName)
                .initialCommissionAmount(BigDecimal.ZERO)
                .maintenanceCommissionAmount(BigDecimal.ZERO)
                .recoveryAmount(BigDecimal.ZERO)
                .totalCommissionAmount(BigDecimal.ZERO)
                .netCommissionAmount(BigDecimal.ZERO)
                .fpCount(0L)
                .contractCount(0L)
                .recoveryContractCount(0L)
                .build();
    }

    public static OrganizationCommissionMonthlyTrendResponse emptyHq(String closingMonth) {
        return OrganizationCommissionMonthlyTrendResponse.builder()
                .scope("HQ")
                .closingMonth(closingMonth)
                .organizationId(null)
                .organizationName(null)
                .initialCommissionAmount(BigDecimal.ZERO)
                .maintenanceCommissionAmount(BigDecimal.ZERO)
                .recoveryAmount(BigDecimal.ZERO)
                .totalCommissionAmount(BigDecimal.ZERO)
                .netCommissionAmount(BigDecimal.ZERO)
                .fpCount(null)
                .contractCount(null)
                .recoveryContractCount(null)
                .build();
    }
}
