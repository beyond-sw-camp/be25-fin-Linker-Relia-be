package com.linker.relia.esgimpact.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class EsgImpactResponse {
    private final String targetMonth;
    private final int consultationCount;
    private final int aiBriefingCount;
    private final int handoverCount;
    @JsonProperty("eSignCount")
    private final int eSignCount;
    private final int paperSavedCount;
    private final BigDecimal co2SavedKg;
    private final BigDecimal seaLevelContribution;
    private final BigDecimal earthTemperatureReduction;
    private final int level;
    private final BigDecimal recoveryRate;
    private final String stage;
    private final List<Activity> activities;

    @Getter
    @Builder
    public static class Activity {
        private final String time;
        private final String type;
        private final String title;
        private final String description;
        private final BigDecimal seaLevelDelta;
    }
}
