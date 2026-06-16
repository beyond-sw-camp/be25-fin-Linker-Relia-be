package com.linker.relia.hr.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class HrClosingSummaryResponse {
    private final String closingMonth;
    private final boolean closed;
    private final long organizationCount;
    private final long userCount;
    private final long fpCount;
    private final LocalDateTime closedAt;
}
