package com.linker.relia.insurance.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class InsuranceCompanyPageResponse {
    private final List<InsuranceCompanyListItemResponse> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public static InsuranceCompanyPageResponse from(Page<InsuranceCompanyListItemResponse> page) {
        return InsuranceCompanyPageResponse.builder()
                .content(page.getContent())
                .page(page.getNumber() + 1)
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
