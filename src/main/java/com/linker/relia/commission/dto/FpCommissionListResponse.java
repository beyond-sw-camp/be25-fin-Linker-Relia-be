package com.linker.relia.commission.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class FpCommissionListResponse {
    private final UUID fpId;
    private final String fpName;
    private final BigDecimal initialCommissionAmount;
    private final BigDecimal maintenanceCommissionAmount;
    private final BigDecimal recoveryAmount;
    private final BigDecimal totalPaymentCommissionAmount;
    private final BigDecimal netCommissionAmount;
    private final long contractCount;
    private final long recoveryContractCount;

    public static FpCommissionListResponse from(FpCommissionListQueryResult result) {
        return FpCommissionListResponse.builder()
                .fpId(result.getFpId())
                .fpName(result.getFpName())
                .initialCommissionAmount(result.getInitialCommissionAmount())
                .maintenanceCommissionAmount(result.getMaintenanceCommissionAmount())
                .recoveryAmount(result.getRecoveryAmount())
                .totalPaymentCommissionAmount(result.getTotalPaymentCommissionAmount())
                .netCommissionAmount(result.getNetCommissionAmount())
                .contractCount(result.getContractCount())
                .recoveryContractCount(result.getRecoveryContractCount())
                .build();
    }
}
