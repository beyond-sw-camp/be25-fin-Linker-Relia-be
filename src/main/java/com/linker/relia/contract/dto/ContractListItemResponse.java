package com.linker.relia.contract.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class ContractListItemResponse {
    private final UUID contractId;
    private final String customerName;
    private final String contractCode;
    private final LocalDate contractDate;
    private final LocalDate contractEndDate;
    private final String contractStatus;
    private final BigDecimal monthlyPremium;
    private final String insuranceProductName;
    private final String insuranceCompanyName;
}
