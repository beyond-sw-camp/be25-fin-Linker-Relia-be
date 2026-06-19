package com.linker.relia.dashboard.dto;

import java.util.UUID;

public record DashboardContractDistributionQueryResult(
        UUID id,
        String name,
        long contractCount
) {
}
