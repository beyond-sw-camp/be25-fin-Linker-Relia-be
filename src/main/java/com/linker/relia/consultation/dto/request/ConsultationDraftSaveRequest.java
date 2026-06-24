package com.linker.relia.consultation.dto.request;

import com.linker.relia.consultation.domain.ConsultationChannel;
import com.linker.relia.consultation.domain.ConsultationType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class ConsultationDraftSaveRequest {
    private UUID customerId;
    private UUID contractId;

    @NotNull
    private ConsultationType consultationType;

    @NotNull
    private ConsultationChannel consultationChannel;

    @NotNull
    private LocalDateTime consultedAt;
    private String specialNote;
    private LocalDateTime nextScheduledAt;

    @NotNull
    private Object draftData;
}
