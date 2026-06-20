package com.linker.relia.consultation.dto.response;

import com.linker.relia.consultation.domain.stt.ConsultationAiNoteStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ConsultationAiNoteApplyResponse {
    private UUID aiNoteId;
    private UUID consultationId;
    private ConsultationAiNoteStatus draftStatus;
    private LocalDateTime appliedAt;
}
