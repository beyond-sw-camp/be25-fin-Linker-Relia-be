package com.linker.relia.contract.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ContractSummaryRequest {
    private String organizationCode;
    private UUID insuranceCompanyId;
}
