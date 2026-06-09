package com.linker.relia.contract.dto;

import java.time.LocalDate;

public record ContractDetailQueryResult(
        String customerName,
        String customerStatus,
        String customerGender,
        LocalDate customerBirthDate,
        String customerPhone,
        String customerEmail,
        String customerAddress,
        String insuranceCompanyName,
        String insuranceProductName,
        LocalDate contractStartDate,
        LocalDate contractEndDate,
        LocalDate coverageStartDate,
        LocalDate coverageEndDate,
        Integer paymentPeriodYears,
        String paymentCycle
) {
}
