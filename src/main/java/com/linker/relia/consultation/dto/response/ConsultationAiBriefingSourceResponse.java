package com.linker.relia.consultation.dto.response;

import com.linker.relia.consultation.domain.ConsultationChannel;
import com.linker.relia.consultation.domain.ConsultationType;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class ConsultationAiBriefingSourceResponse {
    private final UUID consultationId;
    private final Integer consultationSequence;
    private final LocalDateTime consultedAt;
    private final ConsultationType consultationType;
    private final ConsultationChannel consultationChannel;
    private final String fpName;
    private final String specialNote;
    private final LocalDateTime nextScheduledAt;

    public ConsultationAiBriefingSourceResponse(UUID consultationId,
                                                Integer consultationSequence,
                                                LocalDateTime consultedAt,
                                                ConsultationType consultationType,
                                                ConsultationChannel consultationChannel,
                                                String fpName,
                                                String specialNote,
                                                LocalDateTime nextScheduledAt) {
        this.consultationId = consultationId;
        this.consultationSequence = consultationSequence;
        this.consultedAt = consultedAt;
        this.consultationType = consultationType;
        this.consultationChannel = consultationChannel;
        this.fpName = fpName;
        this.specialNote = specialNote;
        this.nextScheduledAt = nextScheduledAt;
    }
}