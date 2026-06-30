package com.linker.relia.esgimpact.repository;

import com.linker.relia.esgimpact.dto.EsgImpactResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EsgImpactStatisticsRepository {
    private final JdbcTemplate jdbcTemplate;

    public List<EsgImpactActivityRow> findMonthlyActivities(UUID userId, LocalDateTime startAt, LocalDateTime endAt) {
        String sql = """
                select occurred_at, activity_type, title, paper_saved
                from (
                    select
                        c.created_at as occurred_at,
                        'CONSULTATION' as activity_type,
                        '상담 일지 작성' as title,
                        3 as paper_saved
                    from consultations c
                    where c.fp_id = ?
                      and c.deleted_at is null
                      and c.created_at >= ?
                      and c.created_at < ?

                    union all

                    select
                        b.created_at as occurred_at,
                        'AI_BRIEFING' as activity_type,
                        'AI 브리핑 생성' as title,
                        5 as paper_saved
                    from consultation_ai_briefings b
                    where b.created_by = ?
                      and b.deleted_at is null
                      and b.created_at >= ?
                      and b.created_at < ?

                    union all

                    select
                        r.approved_at as occurred_at,
                        'HANDOVER' as activity_type,
                        '인수인계 완료' as title,
                        4 as paper_saved
                    from handover_recommendations r
                    join handover_requests h on h.id = r.handover_request_id
                    where r.recommended_fp_id = ?
                      and r.approval_status = 'APPROVED'
                      and r.approved_at is not null
                      and h.request_status = 'COMPLETED'
                      and h.deleted_at is null
                      and r.approved_at >= ?
                      and r.approved_at < ?
                ) activities
                order by occurred_at desc
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new EsgImpactActivityRow(
                        rs.getTimestamp("occurred_at").toLocalDateTime(),
                        rs.getString("activity_type"),
                        rs.getString("title"),
                        rs.getInt("paper_saved")
                ),
                userId.toString(), startAt, endAt,
                userId.toString(), startAt, endAt,
                userId.toString(), startAt, endAt
        );
    }

    public void upsert(UUID userId, EsgImpactResponse response) {
        String sql = """
                insert into esg_impact_statistics (
                    id,
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
                    recovery_rate,
                    created_by,
                    updated_by
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on duplicate key update
                    consultation_count = values(consultation_count),
                    ai_briefing_count = values(ai_briefing_count),
                    handover_count = values(handover_count),
                    e_sign_count = values(e_sign_count),
                    paper_saved_count = values(paper_saved_count),
                    co2_saved_kg = values(co2_saved_kg),
                    sea_level_contribution = values(sea_level_contribution),
                    earth_temperature_reduction = values(earth_temperature_reduction),
                    level = values(level),
                    recovery_rate = values(recovery_rate),
                    updated_by = values(updated_by)
                """;

        jdbcTemplate.update(
                sql,
                UUID.randomUUID().toString(),
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
                response.getRecoveryRate(),
                userId.toString(),
                userId.toString()
        );
    }

    public record EsgImpactActivityRow(
            LocalDateTime occurredAt,
            String type,
            String title,
            int paperSaved
    ) {
    }
}
