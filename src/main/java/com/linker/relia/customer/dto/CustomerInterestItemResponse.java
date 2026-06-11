package com.linker.relia.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.linker.relia.customer.domain.CustomerStatus;
import com.linker.relia.customer.domain.InterestReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
public class CustomerInterestItemResponse {
    private final UUID customerId;
    private final String customerName;
    private final LocalDate customerBirthDate;
    private final String customerPhone;
    private final CustomerStatus customerStatus;
    private final InterestReason interestReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime lastConsultedAt;

    private final Integer unpaidInstallmentCount;
    private final Integer renewalDDay;
    private final Integer maturityDDay;
    private final UUID organizationId;
    private final String organizationCode;
    private final String organizationName;
}
