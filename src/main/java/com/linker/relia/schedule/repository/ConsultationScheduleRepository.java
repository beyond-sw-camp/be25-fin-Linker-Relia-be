package com.linker.relia.schedule.repository;

import com.linker.relia.schedule.domain.ConsultationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultationScheduleRepository extends JpaRepository<ConsultationSchedule, UUID> {

    Optional<ConsultationSchedule> findByIdAndDeletedAtIsNull(UUID scheduleId);

    List<ConsultationSchedule> findAllByFp_IdAndScheduledAtBetweenAndDeletedAtIsNullOrderByScheduledAtAsc(
            UUID fpId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<ConsultationSchedule> findAllByFp_IdAndScheduledAtBetweenAndDeletedAtIsNull(
            UUID fpId,
            LocalDateTime start,
            LocalDateTime end
    );
}