package com.linker.relia.commission.dto;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class OrganizationCommissionListResponse {
    private final UUID organizationId;
    private final String organizationName;
    private final BigDecimal initialCommissionAmount;
    private final BigDecimal maintenanceCommissionAmount;
    private final BigDecimal recoveryAmount;
    private final BigDecimal totalPaymentCommissionAmount;
    private final BigDecimal netCommissionAmount;
    private final long fpCount;
    private final long contractCount;
    private final long recoveryContractCount;

    public static OrganizationCommissionListResponse from(OrganizationCommissionListQueryResult result) {
        return OrganizationCommissionListResponse.builder()
                .organizationId(result.getOrganizationId())
                .organizationName(result.getOrganizationName())
                .initialCommissionAmount(result.getInitialCommissionAmount())
                .maintenanceCommissionAmount(result.getMaintenanceCommissionAmount())
                .recoveryAmount(result.getRecoveryAmount())
                .totalPaymentCommissionAmount(result.getTotalPaymentCommissionAmount())
                .netCommissionAmount(result.getNetCommissionAmount())
                .fpCount(result.getFpCount())
                .contractCount(result.getContractCount())
                .recoveryContractCount(result.getRecoveryContractCount())
                .build();
    }
}
