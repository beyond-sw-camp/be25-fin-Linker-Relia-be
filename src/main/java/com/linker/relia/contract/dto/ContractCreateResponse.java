package com.linker.relia.contract.dto;

import com.linker.relia.contract.domain.Contract;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ContractCreateResponse {
    private final UUID contractId;
    private final String contractCode;
    private final String contractStatus;
    private final LocalDateTime createdAt;

    public static ContractCreateResponse from(Contract contract) {
        return ContractCreateResponse.builder()
                .contractId(contract.getId())
                .contractCode(contract.getContractCode())
                .contractStatus(contract.getContractStatus())
                .createdAt(contract.getCreatedAt())
                .build();
    }
}
