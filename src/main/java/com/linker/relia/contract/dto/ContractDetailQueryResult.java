package com.linker.relia.contract.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContractDetailQueryResult(
        String contractCode,
        String contractStatus,
        String customerName,
        String customerStatus,
        String customerGender,
        LocalDate customerBirthDate,
        String customerPhone,
        String customerEmail,
        String customerAddress,
        String customerJob,
        String customerCompanyName,
        String insuranceCompanyName,
        String insuranceCategoryName,
        String insuranceProductName,
        LocalDate contractDate,
        LocalDate contractStartDate,
        LocalDate contractEndDate,
        LocalDate coverageStartDate,
        LocalDate coverageEndDate,
        Integer paymentPeriodYears,
        String paymentCycle,
        BigDecimal monthlyPremium,
        String coverageSummary,
        String fpName,
        String fpOrganizationName,
        LocalDateTime createdAt
) {
}
