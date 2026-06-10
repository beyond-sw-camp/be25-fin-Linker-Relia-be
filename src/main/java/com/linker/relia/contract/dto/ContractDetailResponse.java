package com.linker.relia.contract.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ContractDetailResponse {
    private final ContractSummary contractSummary;
    private final CustomerInfo customerInfo;
    private final ContractInfo contractInfo;

    @Getter
    @Builder
    public static class ContractSummary {
        private final String customerName;
        private final String customerStatus;
        private final String insuranceCompanyName;
        private final String insuranceProductName;
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
    }

    @Getter
    @Builder
    public static class ContractInfo {
        private final String insuranceCompanyName;
        private final String insuranceProductName;
        private final LocalDate contractStartDate;
        private final LocalDate contractEndDate;
        private final LocalDate coverageStartDate;
        private final LocalDate coverageEndDate;
        private final Integer paymentPeriodYears;
        private final String paymentCycle;
    }
}
