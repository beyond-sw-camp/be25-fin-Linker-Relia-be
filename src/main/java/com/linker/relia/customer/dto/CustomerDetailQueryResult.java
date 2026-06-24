package com.linker.relia.customer.dto;

import com.linker.relia.customer.domain.CustomerGrade;
import com.linker.relia.customer.domain.InterestReason;
import com.linker.relia.customer.domain.CustomerStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerDetailQueryResult(
        UUID customerId,
        String customerName,
        CustomerStatus customerStatus,
        boolean interestYn,
        InterestReason interestReason,
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
        LocalDateTime nextConsultedAt,
        LocalDate contractEndDate,
        Integer unpaidInstallmentCount,
        Integer renewalDDay,
        Integer maturityDDay
) {
}
