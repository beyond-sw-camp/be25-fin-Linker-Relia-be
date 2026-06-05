package com.linker.relia.customer.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CustomerListResponse {
    private final CustomerListSummaryResponse summary;
    private final List<CustomerListItemResponse> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final int numberOfElements;
    private final boolean hasNext;
    private final boolean hasPrevious;
    private final boolean first;
    private final boolean last;
    private final boolean empty;
}
