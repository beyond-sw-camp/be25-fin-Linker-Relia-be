package com.linker.relia.customer.dto;

import com.linker.relia.common.dto.response.PageResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerListResponse {
    private final CustomerListSummaryResponse summary;
    private final PageResponse<CustomerListItemResponse> customers;
}
