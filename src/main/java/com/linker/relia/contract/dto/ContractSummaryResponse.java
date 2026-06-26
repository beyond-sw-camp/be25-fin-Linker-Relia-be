package com.linker.relia.contract.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContractSummaryResponse {
    private final long totalContractCount;
    private final long normalPaymentCount;
    private final long unpaidCount;
    private final long lapseExpectedCount;
    private final long expiringSoonCount;
    private final long renewalSoonCount;

    public static ContractSummaryResponse empty() {
        return ContractSummaryResponse.builder()
                .totalContractCount(0L)
                .normalPaymentCount(0L)
                .unpaidCount(0L)
                .lapseExpectedCount(0L)
                .expiringSoonCount(0L)
                .renewalSoonCount(0L)
                .build();
    }
}
