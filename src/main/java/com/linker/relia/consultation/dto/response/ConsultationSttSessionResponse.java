package com.linker.relia.consultation.dto.response;

import com.linker.relia.consultation.domain.ConsultationType;
import com.linker.relia.consultation.domain.stt.ConsultationSttSessionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ConsultationSttSessionResponse {
    private UUID sessionId;
    private UUID customerId;
    private ConsultationType consultationType;
    private ConsultationSttSessionStatus sessionStatus;
    private String partialText;
    private String finalText;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
