package com.linker.relia.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class CustomerInterestSummaryResponse {
    private final long totalInterestCustomerCount;
    private final long unpaidInterestCustomerCount;
    private final long renewalDueInterestCustomerCount;
    private final long maturityDueInterestCustomerCount;
}
