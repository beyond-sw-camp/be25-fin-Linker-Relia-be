package com.linker.relia.dashboard.dto;

public record DashboardContractStatusQueryResult(
        long totalContractCount,
        long maintenanceContractCount,
        long lapsedContractCount,
        long terminatedContractCount,
        long completedContractCount
) {
}
