package com.linker.relia.customer.dto;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class CustomerAiBriefingResponse {
    private final UUID id;
    private final String briefingContent;
    private final LocalDateTime createdAt;
    private final boolean canGenerate;

    public CustomerAiBriefingResponse(UUID id,
                                      String briefingContent,
                                      LocalDateTime createdAt,
                                      boolean canGenerate) {
        this.id = id;
        this.briefingContent = briefingContent;
        this.createdAt = createdAt;
        this.canGenerate = canGenerate;
    }

    public static CustomerAiBriefingResponse empty() {
        return new CustomerAiBriefingResponse(null, "", null, true);
    }
}
