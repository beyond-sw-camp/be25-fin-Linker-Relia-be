package com.linker.relia.schedule.dto.request;

import com.linker.relia.consultation.domain.ConsultationChannel;
import com.linker.relia.consultation.domain.ConsultationType;
import com.linker.relia.schedule.domain.ScheduleStatus;
import com.linker.relia.schedule.domain.ScheduleType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class ConsultationScheduleUpdateRequest {

    private ScheduleType scheduleType;

    private UUID customerId;

    private UUID contractId;

    private UUID consultationId;

    private ConsultationType consultationType;

    private ConsultationChannel consultationChannel;

    private String title;

    private String memo;

    private LocalDateTime scheduledAt;

    private ScheduleStatus scheduleStatus;
}