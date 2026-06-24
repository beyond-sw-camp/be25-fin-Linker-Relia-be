package com.linker.relia.hr.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class HrClosingProcessResponse {
    private final String closingMonth;
    private final long organizationCount;
    private final long userCount;
    private final long fpCount;
    private final LocalDateTime closedAt;
}
