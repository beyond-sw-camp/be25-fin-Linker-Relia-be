package com.linker.relia.organization.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class FpContractListItemResponse {
    private final String customerName;
    private final String insuranceType;
    private final String insuranceCompany;
    private final LocalDate contractDate;
    private final BigDecimal monthlyPremium;
    private final String contractStatus;
}
