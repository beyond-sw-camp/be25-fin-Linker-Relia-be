package com.linker.relia.contract.dto;

import java.math.BigDecimal;

public record CustomerContractSummaryRow(
        long totalContractCount,
        BigDecimal totalMonthlyPremium,
        long maintenanceCount,
        long completedCount,
        long terminatedCount,
        long lapsedCount
) {
}
