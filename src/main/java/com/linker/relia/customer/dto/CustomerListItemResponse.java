package com.linker.relia.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.linker.relia.customer.domain.CustomerGrade;
import com.linker.relia.customer.domain.CustomerStatus;
import com.linker.relia.customer.domain.InterestReason;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CustomerListItemResponse {
    private final UUID customerId;
    private final String customerName;
    private final LocalDate customerBirthDate;
    private final String customerPhone;
    private final long contractCount;
    private final BigDecimal monthlyPremium;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime lastConsultedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime nextConsultedAt;

    private final CustomerGrade customerGrade;
    private final CustomerStatus customerStatus;
    private final boolean interestYn;
    private final InterestReason interestReason;
    private final UUID organizationId;
    private final String organizationCode;
    private final String organizationName;
}
