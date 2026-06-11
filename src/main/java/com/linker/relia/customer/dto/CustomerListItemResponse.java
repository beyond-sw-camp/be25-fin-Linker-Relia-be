package com.linker.relia.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.linker.relia.customer.domain.CustomerGrade;
import com.linker.relia.customer.domain.CustomerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
public class CustomerListItemResponse {
    private final UUID customerId;
    private final String customerName;
    private final LocalDate customerBirthDate;
    private final String customerPhone;
    private final long contractCount;
    private final BigDecimal monthlyPremium;
    private final LocalDate contractEndDate;
    private final LocalDate terminatedAt;
    private final LocalDateTime lastConsultedAt;
    private final LocalDateTime nextConsultedAt;
    private final CustomerGrade customerGrade;
    private final CustomerStatus customerStatus;
    private final UUID organizationId;
    private final String organizationCode;
    private final String organizationName;
}
