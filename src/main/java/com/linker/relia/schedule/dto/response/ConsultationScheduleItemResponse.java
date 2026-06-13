package com.linker.relia.schedule.dto.response;

import com.linker.relia.consultation.domain.ConsultationChannel;
import com.linker.relia.consultation.domain.ConsultationType;
import com.linker.relia.schedule.domain.ScheduleStatus;
import com.linker.relia.schedule.domain.ScheduleType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ConsultationScheduleItemResponse {

    private UUID scheduleId;

    private ScheduleType scheduleType;

    private UUID customerId;

    private String customerName;

    private UUID contractId;

    private UUID consultationId;

    private ConsultationType consultationType;

    private ConsultationChannel consultationChannel;

    private String title;

    private String memo;

    private LocalDateTime scheduledAt;

    private ScheduleStatus status;
}