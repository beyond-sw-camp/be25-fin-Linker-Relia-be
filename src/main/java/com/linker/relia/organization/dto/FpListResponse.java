package com.linker.relia.organization.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class FpListResponse {
    private final List<FpListItemResponse> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    /**
     * Create an FpListResponse DTO from a Spring Data Page of FpListItemResponse.
     *
     * The returned DTO's `page` value is converted to 1-based index (page.getNumber() + 1).
     *
     * @param page the Spring Data Page containing FpListItemResponse items used to populate the DTO
     * @return an FpListResponse populated with content, page (1-based), size, totalElements and totalPages
     */
    public static FpListResponse from(Page<FpListItemResponse> page) {
        return FpListResponse.builder()
                .content(page.getContent())
                .page(page.getNumber() + 1)
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
