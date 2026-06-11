package com.linker.relia.handover.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record HandoverListResponse(
        long totalCount,
        List<HandoverListItemResponse> handovers
) {
    public static HandoverListResponse of(Page<HandoverListItemResponse> page) {
        return new HandoverListResponse(
                page.getTotalElements(),
                page.getContent()
        );
    }
}