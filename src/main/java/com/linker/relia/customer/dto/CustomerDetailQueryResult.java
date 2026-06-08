package com.linker.relia.customer.dto;

import com.linker.relia.customer.domain.CustomerGrade;
import com.linker.relia.customer.domain.CustomerStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerDetailQueryResult(
        UUID customerId,
        String customerName,
        CustomerStatus customerStatus,
        boolean interestYn,
        CustomerGrade customerGrade,
        LocalDate customerBirthDate,
        String customerGender,
        String customerPhone,
        String customerEmail,
        String customerAddress,
        String customerJob,
        String customerCompanyName,
        UUID fpId,
        String fpName,
        String organizationCode,
        String organizationName,
        LocalDateTime lastConsultedAt,
        LocalDateTime nextConsultedAt
) {
}
