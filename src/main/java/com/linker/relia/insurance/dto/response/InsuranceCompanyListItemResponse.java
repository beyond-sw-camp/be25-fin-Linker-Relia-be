package com.linker.relia.insurance.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class InsuranceCompanyListItemResponse {
    private final UUID insuranceCompanyId;
    private final String insuranceCompanyName;
    private final LocalDate partnerStartDate;
    private final Long incomeCommission;
    private final Long contractCount;
}
