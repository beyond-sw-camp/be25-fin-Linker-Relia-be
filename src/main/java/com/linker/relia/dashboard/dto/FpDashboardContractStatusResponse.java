package com.linker.relia.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class FpDashboardContractStatusResponse {
    private final LocalDate referenceDate;
    private final String closingMonth;
    private final long totalContractCount;
    private final long maintenanceContractCount;
    private final long lapsedContractCount;
    private final long terminatedContractCount;
    private final long completedContractCount;
}
