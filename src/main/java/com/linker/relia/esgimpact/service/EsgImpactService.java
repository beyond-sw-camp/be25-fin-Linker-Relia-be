package com.linker.relia.esgimpact.service;

import com.linker.relia.esgimpact.dto.EsgImpactResponse;
import com.linker.relia.esgimpact.repository.EsgImpactStatisticsRepository;
import com.linker.relia.esgimpact.repository.EsgImpactStatisticsRepository.EsgImpactActivityRow;
import com.linker.relia.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EsgImpactService {
    private static final String CONSULTATION = "CONSULTATION";
    private static final String AI_BRIEFING = "AI_BRIEFING";
    private static final String HANDOVER = "HANDOVER";

    private static final BigDecimal CO2_SAVED_PER_PAPER = new BigDecimal("0.015");
    private static final BigDecimal SEA_LEVEL_CONTRIBUTION_PER_PAPER = new BigDecimal("0.00037");
    private static final BigDecimal RECOVERY_TARGET_PAPER_COUNT = new BigDecimal("320");
    private static final DateTimeFormatter ACTIVITY_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final EsgImpactStatisticsRepository esgImpactStatisticsRepository;

    @Transactional
    public EsgImpactResponse getMyImpact(PrincipalDetails principalDetails, String targetMonth) {
        UUID userId = principalDetails.getUser().getId();
        YearMonth resolvedTargetMonth = resolveTargetMonth(targetMonth);

        List<EsgImpactActivityRow> activityRows = esgImpactStatisticsRepository.findMonthlyActivities(
                userId,
                resolvedTargetMonth.atDay(1).atStartOfDay(),
                resolvedTargetMonth.plusMonths(1).atDay(1).atStartOfDay()
        );

        EsgImpactResponse response = calculate(resolvedTargetMonth.toString(), activityRows);
        esgImpactStatisticsRepository.upsert(userId, response);
        return response;
    }

    private EsgImpactResponse calculate(String targetMonth, List<EsgImpactActivityRow> activityRows) {
        int consultationCount = count(activityRows, CONSULTATION);
        int aiBriefingCount = count(activityRows, AI_BRIEFING);
        int handoverCount = count(activityRows, HANDOVER);
        int eSignCount = 0;
        int paperSavedCount = activityRows.stream()
                .mapToInt(EsgImpactActivityRow::paperSaved)
                .sum();

        BigDecimal paperSaved = BigDecimal.valueOf(paperSavedCount);
        BigDecimal recoveryRate = paperSaved
                .multiply(BigDecimal.valueOf(100))
                .divide(RECOVERY_TARGET_PAPER_COUNT, 2, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100));

        return EsgImpactResponse.builder()
                .targetMonth(targetMonth)
                .consultationCount(consultationCount)
                .aiBriefingCount(aiBriefingCount)
                .handoverCount(handoverCount)
                .eSignCount(eSignCount)
                .paperSavedCount(paperSavedCount)
                .co2SavedKg(paperSaved.multiply(CO2_SAVED_PER_PAPER).setScale(3, RoundingMode.HALF_UP))
                .seaLevelContribution(paperSaved.multiply(SEA_LEVEL_CONTRIBUTION_PER_PAPER).setScale(5, RoundingMode.HALF_UP))
                .earthTemperatureReduction(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP))
                .level(calculateLevel(paperSavedCount))
                .recoveryRate(recoveryRate)
                .stage(calculateStage(recoveryRate))
                .activities(toActivities(activityRows))
                .build();
    }

    private int count(List<EsgImpactActivityRow> activityRows, String type) {
        return (int) activityRows.stream()
                .filter(row -> type.equals(row.type()))
                .count();
    }

    private int calculateLevel(int paperSavedCount) {
        return Math.max(1, paperSavedCount / 60 + 1);
    }

    private String calculateStage(BigDecimal recoveryRate) {
        if (recoveryRate.compareTo(BigDecimal.valueOf(25)) <= 0) {
            return "시작 단계";
        }
        if (recoveryRate.compareTo(BigDecimal.valueOf(50)) <= 0) {
            return "안정 단계";
        }
        if (recoveryRate.compareTo(BigDecimal.valueOf(75)) <= 0) {
            return "회복 단계";
        }
        return "고도 회복 단계";
    }

    private List<EsgImpactResponse.Activity> toActivities(List<EsgImpactActivityRow> activityRows) {
        return activityRows.stream()
                .map(row -> EsgImpactResponse.Activity.builder()
                        .time(row.occurredAt().format(ACTIVITY_TIME_FORMATTER))
                        .type(row.type())
                        .title(row.title())
                        .description("종이 " + row.paperSaved() + "장 절감")
                        .seaLevelDelta(BigDecimal.valueOf(row.paperSaved())
                                .multiply(SEA_LEVEL_CONTRIBUTION_PER_PAPER)
                                .negate()
                                .setScale(5, RoundingMode.HALF_UP))
                        .build())
                .toList();
    }

    private YearMonth resolveTargetMonth(String targetMonth) {
        if (targetMonth == null || targetMonth.isBlank()) {
            return YearMonth.now();
        }

        try {
            return YearMonth.parse(targetMonth.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("targetMonth는 YYYY-MM 형식이어야 합니다.");
        }
    }
}
