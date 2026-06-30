package com.linker.relia.esgimpact.service;

import com.linker.relia.esgimpact.dto.EsgImpactResponse;
import com.linker.relia.esgimpact.repository.EsgImpactStatisticsRepository;
import com.linker.relia.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EsgImpactService {
    private static final int DEMO_CONSULTATION_COUNT = 12;
    private static final int DEMO_AI_BRIEFING_COUNT = 5;
    private static final int DEMO_HANDOVER_COUNT = 2;
    private static final int DEMO_E_SIGN_COUNT = 8;

    private static final int CONSULTATION_PAPER_SAVED = 3;
    private static final int AI_BRIEFING_PAPER_SAVED = 5;
    private static final int HANDOVER_PAPER_SAVED = 4;
    private static final int E_SIGN_PAPER_SAVED = 2;

    private static final BigDecimal CO2_SAVED_PER_PAPER = new BigDecimal("0.015");
    private static final BigDecimal SEA_LEVEL_CONTRIBUTION_PER_PAPER = new BigDecimal("0.00037");
    private static final BigDecimal RECOVERY_TARGET_PAPER_COUNT = new BigDecimal("320");

    @Transactional
    public EsgImpactResponse getMyImpact(PrincipalDetails principalDetails, String targetMonth) {
        UUID userId = principalDetails.getUser().getId();
        String resolvedTargetMonth = resolveTargetMonth(targetMonth);

        return esgImpactStatisticsRepository.findByUserIdAndTargetMonth(userId, resolvedTargetMonth)
                .orElseGet(() -> createDemoImpact(userId, resolvedTargetMonth));
    }

    private final EsgImpactStatisticsRepository esgImpactStatisticsRepository;

    private EsgImpactResponse createDemoImpact(UUID userId, String targetMonth) {
        EsgImpactResponse demoImpact = calculate(
                targetMonth,
                DEMO_CONSULTATION_COUNT,
                DEMO_AI_BRIEFING_COUNT,
                DEMO_HANDOVER_COUNT,
                DEMO_E_SIGN_COUNT
        );

        esgImpactStatisticsRepository.insert(userId, demoImpact);
        return esgImpactStatisticsRepository.findByUserIdAndTargetMonth(userId, targetMonth)
                .orElse(demoImpact);
    }

    private EsgImpactResponse calculate(
            String targetMonth,
            int consultationCount,
            int aiBriefingCount,
            int handoverCount,
            int eSignCount
    ) {
        int paperSavedCount =
                consultationCount * CONSULTATION_PAPER_SAVED
                        + aiBriefingCount * AI_BRIEFING_PAPER_SAVED
                        + handoverCount * HANDOVER_PAPER_SAVED
                        + eSignCount * E_SIGN_PAPER_SAVED;

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
                .level(Math.max(1, paperSavedCount / 60 + 1))
                .recoveryRate(recoveryRate)
                .build();
    }

    private String resolveTargetMonth(String targetMonth) {
        if (targetMonth == null || targetMonth.isBlank()) {
            return YearMonth.now().toString();
        }

        try {
            return YearMonth.parse(targetMonth.trim()).toString();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("targetMonth는 YYYY-MM 형식이어야 합니다.");
        }
    }
}
