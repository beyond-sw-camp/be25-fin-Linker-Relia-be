package com.linker.relia.customer.dto;

import com.linker.relia.customer.domain.CustomerGrade;
import com.linker.relia.customer.domain.CustomerStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class CustomerDetailResponse {
    private UUID customerId;
    private String customerName;
    private CustomerStatus customerStatus;
    private boolean interestYn;
    private CustomerGrade customerGrade;
    private LocalDate customerBirthDate;
    private String customerGender;
    private String customerPhone;
    private String customerEmail;
    private String customerAddress;
    private String customerJob;
    private String customerCompanyName;
    private UUID fpId;
    private String fpName;
    private String organizationCode;
    private String organizationName;
    private LocalDate lastConsultedAt;
    private LocalDate nextConsultedAt;
    private CustomerContractSummaryResponse contractSummary;
}
