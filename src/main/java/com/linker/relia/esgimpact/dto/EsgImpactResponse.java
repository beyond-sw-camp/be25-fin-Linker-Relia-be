package com.linker.relia.esgimpact.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class EsgImpactResponse {
    private final String targetMonth;
    private final int consultationCount;
    private final int aiBriefingCount;
    private final int handoverCount;
    private final int eSignCount;
    private final int paperSavedCount;
    private final BigDecimal co2SavedKg;
    private final BigDecimal seaLevelContribution;
    private final BigDecimal earthTemperatureReduction;
    private final int level;
    private final BigDecimal recoveryRate;
}
