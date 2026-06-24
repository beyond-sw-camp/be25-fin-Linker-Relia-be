package com.linker.relia.consultation.dto.request;

import com.linker.relia.consultation.domain.ConsultationNewCoverageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
public class ConsultationNewDetailRequest {
    private BigDecimal monthlyIncome;

    private Boolean hasExistingInsurance;

    private BigDecimal monthlyInsurancePremium;

    private String existingInsuranceNote;

    private String insurancePriority;

    private List<@NotNull ConsultationNewCoverageType> coverageTypes;

    private List<@NotBlank String> proposedProductCodes;
}
