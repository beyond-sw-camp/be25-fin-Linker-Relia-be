package com.linker.relia.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class OrganizationDashboardContractDistributionResponse {
    private final String closingMonth;
    private final String organizationCode;
    private final String organizationName;
    private final long totalContractCount;
    private final List<InsuranceCompanyDistributionItem> insuranceCompanies;
    private final List<InsuranceCategoryDistributionItem> insuranceCategories;

    @Getter
    @Builder
    public static class InsuranceCompanyDistributionItem {
        private final UUID insuranceCompanyId;
        private final String insuranceCompanyName;
        private final long contractCount;
        private final BigDecimal contractRatio;
    }

    @Getter
    @Builder
    public static class InsuranceCategoryDistributionItem {
        private final UUID insuranceCategoryId;
        private final String insuranceCategoryName;
        private final long contractCount;
        private final BigDecimal contractRatio;
    }
}
