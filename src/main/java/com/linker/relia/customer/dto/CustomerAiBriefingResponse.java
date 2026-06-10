package com.linker.relia.customer.dto;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class CustomerAiBriefingResponse {
    private final UUID id;
    private final String briefingContent;
    private final LocalDateTime createdAt;

    public CustomerAiBriefingResponse(UUID id, String briefingContent, LocalDateTime createdAt) {
        this.id = id;
        this.briefingContent = briefingContent;
        this.createdAt = createdAt;
    }

    public static CustomerAiBriefingResponse empty() {
        return new CustomerAiBriefingResponse(null, "", null);
    }
}
