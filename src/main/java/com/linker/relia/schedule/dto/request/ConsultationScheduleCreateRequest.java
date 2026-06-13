package com.linker.relia.schedule.dto.request;

import com.linker.relia.consultation.domain.ConsultationChannel;
import com.linker.relia.consultation.domain.ConsultationType;
import com.linker.relia.schedule.domain.ScheduleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class ConsultationScheduleCreateRequest {

    @NotNull(message = "일정 유형은 필수입니다.")
    private ScheduleType scheduleType;

    @NotNull(message = "고객 ID는 필수입니다.")
    private UUID customerId;

    private UUID contractId;

    private UUID consultationId;

    private ConsultationType consultationType;

    private ConsultationChannel consultationChannel;

    @NotBlank(message = "일정 제목은 필수입니다.")
    private String title;

    private String memo;

    @NotNull(message = "일정 일시는 필수입니다.")
    private LocalDateTime scheduledAt;
}