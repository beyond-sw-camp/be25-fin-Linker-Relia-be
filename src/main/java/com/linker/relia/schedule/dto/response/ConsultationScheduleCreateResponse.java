package com.linker.relia.schedule.dto.response;

import com.linker.relia.schedule.domain.ScheduleStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ConsultationScheduleCreateResponse {

    private UUID scheduleId;

    private LocalDateTime scheduledAt;

    private ScheduleStatus status;
}