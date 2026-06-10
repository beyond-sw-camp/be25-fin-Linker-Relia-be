package com.linker.relia.consultation.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class ConsultationNewDetailRequest {
    private BigDecimal monthlyIncome;

    private Boolean hasExistingInsurance;

    private BigDecimal monthlyInsurancePremium;

    private String existingInsuranceNote;

    private String InsurancePriority;

    private List<String> coverageTypes;

    private List<UUID> proposedProductIds;
}
