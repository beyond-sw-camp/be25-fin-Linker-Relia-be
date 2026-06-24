package com.linker.relia.contract.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ContractDetailResponse {
    private final ContractSummary contractSummary;
    private final CustomerInfo customerInfo;
    private final ContractInfo contractInfo;
    private final CoverageInfo coverageInfo;

    @Getter
    @Builder
    public static class ContractSummary {
        private final String contractCode;
        private final String customerName;
        private final String customerStatus;
        private final String insuranceCompanyName;
        private final String insuranceProductName;
        private final String contractStatus;
        private final BigDecimal monthlyPremium;
        private final LocalDate contractStartDate;
        private final LocalDate contractEndDate;
        private final Integer paymentPeriodYears;
        private final String paymentCycle;
    }

    @Getter
    @Builder
    public static class CustomerInfo {
        private final String customerName;
        private final String customerGender;
        private final LocalDate customerBirthDate;
        private final String customerPhone;
        private final String customerEmail;
        private final String customerAddress;
        private final String customerJob;
        private final String customerCompanyName;
    }

    @Getter
    @Builder
    public static class ContractInfo {
        private final String insuranceCompanyName;
        private final String insuranceCategoryName;
        private final String insuranceProductName;
        private final LocalDate contractDate;
        private final LocalDate contractStartDate;
        private final LocalDate contractEndDate;
        private final LocalDate coverageStartDate;
        private final LocalDate coverageEndDate;
        private final Integer paymentPeriodYears;
        private final String paymentCycle;
        private final BigDecimal monthlyPremium;
        private final String fpName;
        private final String fpOrganizationName;
        private final LocalDateTime createdAt;
    }

    @Getter
    @Builder
    public static class CoverageInfo {
        private final String coverageSummary;
    }
}
