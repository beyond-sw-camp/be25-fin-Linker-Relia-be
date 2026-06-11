package com.linker.relia.customer.dto;

import com.linker.relia.common.dto.response.PageResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerInterestListResponse {
    private final CustomerInterestSummaryResponse summary;
    private final PageResponse<CustomerInterestItemResponse> customers;
}
