package com.linker.relia.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class FpDashboardContractDistributionResponse {
    private final LocalDate referenceDate;
    private final String closingMonth;
    private final long totalContractCount;
    private final List<InsuranceCompanyContractCountItem> insuranceCompanies;
    private final List<InsuranceCategoryContractCountItem> insuranceCategories;

    @Getter
    @Builder
    public static class InsuranceCompanyContractCountItem {
        private final UUID insuranceCompanyId;
        private final String insuranceCompanyName;
        private final long contractCount;
    }

    @Getter
    @Builder
    public static class InsuranceCategoryContractCountItem {
        private final UUID insuranceCategoryId;
        private final String insuranceCategoryName;
        private final long contractCount;
    }
}
