package com.linker.relia.insurance.dto.response;

import com.linker.relia.insurance.domain.InsuranceCompany;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class InsuranceManagementCompanyListItemResponse {
    private final UUID insuranceCompanyId;
    private final String insuranceCompanyName;
    private final String insuranceCompanyPhone;
    private final String insuranceCompanyStatus;
    private final LocalDate partnerStartedAt;
    private final LocalDate partnerTerminatedAt;

    public static InsuranceManagementCompanyListItemResponse from(InsuranceCompany insuranceCompany) {
        return InsuranceManagementCompanyListItemResponse.builder()
                .insuranceCompanyId(insuranceCompany.getId())
                .insuranceCompanyName(insuranceCompany.getInsuranceCompanyName())
                .insuranceCompanyPhone(insuranceCompany.getInsuranceCompanyPhone())
                .insuranceCompanyStatus(insuranceCompany.getInsuranceCompanyStatus())
                .partnerStartedAt(insuranceCompany.getCreatedAt() == null ? null : insuranceCompany.getCreatedAt().toLocalDate())
                .partnerTerminatedAt(insuranceCompany.getDeletedAt() == null ? null : insuranceCompany.getDeletedAt().toLocalDate())
                .build();
    }
}
