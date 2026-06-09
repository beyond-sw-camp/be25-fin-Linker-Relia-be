package com.linker.relia.consultation.dto.response;

import com.linker.relia.consultation.domain.ConsultationChannel;
import com.linker.relia.consultation.domain.ConsultationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ConsultationDraftResponse {
    private UUID draftId;
    private UUID customerId;
    private UUID contractId;
    private ConsultationType consultationType;
    private ConsultationChannel consultationChannel;
    private LocalDateTime consultedAt;
    private String specialNote;
    private LocalDateTime nextScheduledAt;
    private String draftData;
    private LocalDateTime lastSavedAt;
}
