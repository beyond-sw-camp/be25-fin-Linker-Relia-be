package com.linker.relia.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class CustomerListSummaryResponse {
    private final long totalCustomerCount;
    private final long contractedCustomerCount;
    private final long prospectCustomerCount;
    private final long interestCustomerCount;
}
