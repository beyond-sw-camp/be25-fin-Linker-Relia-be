package com.linker.relia.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CustomerOwnedContractResponse {
    private final UUID contractId;
    private final String insuranceCompanyName;
    private final String insuranceProductName;
    private final BigDecimal monthlyPremium;
    private final LocalDate contractStartedAt;
    private final String contractStatus;
}
