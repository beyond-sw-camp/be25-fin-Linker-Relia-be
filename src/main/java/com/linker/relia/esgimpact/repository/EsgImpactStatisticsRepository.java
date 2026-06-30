package com.linker.relia.esgimpact.repository;

import com.linker.relia.esgimpact.dto.EsgImpactResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EsgImpactStatisticsRepository {
    private final JdbcTemplate jdbcTemplate;

    public Optional<EsgImpactResponse> findByUserIdAndTargetMonth(UUID userId, String targetMonth) {
        String sql = """
                select
                    target_month,
                    consultation_count,
                    ai_briefing_count,
                    handover_count,
                    e_sign_count,
                    paper_saved_count,
                    co2_saved_kg,
                    sea_level_contribution,
                    earth_temperature_reduction,
                    level,
                    recovery_rate
                from esg_impact_statistics
                where user_id = ?
                  and target_month = ?
                """;

        return jdbcTemplate.query(sql, this::mapRow, userId.toString(), targetMonth)
                .stream()
                .findFirst();
    }

    public void insert(UUID userId, EsgImpactResponse response) {
        String sql = """
                insert into esg_impact_statistics (
                    user_id,
                    target_month,
                    consultation_count,
                    ai_briefing_count,
                    handover_count,
                    e_sign_count,
                    paper_saved_count,
                    co2_saved_kg,
                    sea_level_contribution,
                    earth_temperature_reduction,
                    level,
                    recovery_rate
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try {
            jdbcTemplate.update(
                    sql,
                    userId.toString(),
                    response.getTargetMonth(),
                    response.getConsultationCount(),
                    response.getAiBriefingCount(),
                    response.getHandoverCount(),
                    response.getESignCount(),
                    response.getPaperSavedCount(),
                    response.getCo2SavedKg(),
                    response.getSeaLevelContribution(),
                    response.getEarthTemperatureReduction(),
                    response.getLevel(),
                    response.getRecoveryRate()
            );
        } catch (DuplicateKeyException ignored) {
            // Another request created the same demo row first. The service re-reads it.
        }
    }

    private EsgImpactResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        return EsgImpactResponse.builder()
                .targetMonth(rs.getString("target_month"))
                .consultationCount(rs.getInt("consultation_count"))
                .aiBriefingCount(rs.getInt("ai_briefing_count"))
                .handoverCount(rs.getInt("handover_count"))
                .eSignCount(rs.getInt("e_sign_count"))
                .paperSavedCount(rs.getInt("paper_saved_count"))
                .co2SavedKg(rs.getBigDecimal("co2_saved_kg"))
                .seaLevelContribution(rs.getBigDecimal("sea_level_contribution"))
                .earthTemperatureReduction(rs.getBigDecimal("earth_temperature_reduction"))
                .level(rs.getInt("level"))
                .recoveryRate(rs.getBigDecimal("recovery_rate"))
                .build();
    }
}
