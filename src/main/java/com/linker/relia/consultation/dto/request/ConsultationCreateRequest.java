package com.linker.relia.consultation.dto.request;

import com.linker.relia.consultation.domain.ConsultationChannel;
import com.linker.relia.consultation.domain.ConsultationType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class ConsultationCreateRequest {

    @NotNull(message = "고객 ID는 필수입니다.")
    private UUID customerId;

    private UUID contractId;

    @NotNull(message = "상담 유형은 필수입니다.")
    private ConsultationType consultationType;

    @NotNull(message = "상담 채널은 필수입니다.")
    private ConsultationChannel consultationChannel;

    @NotNull(message = "상담 일시는 필수입니다.")
    private LocalDateTime consultedAt;

    private String specialNote;

    private LocalDateTime nextScheduledAt;
}