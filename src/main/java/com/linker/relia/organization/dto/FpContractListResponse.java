package com.linker.relia.organization.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class FpContractListResponse {
    private final List<FpContractListItemResponse> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public static FpContractListResponse from(Page<FpContractListItemResponse> page) {
        return FpContractListResponse.builder()
                .content(page.getContent())
                .page(page.getNumber() + 1)
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
