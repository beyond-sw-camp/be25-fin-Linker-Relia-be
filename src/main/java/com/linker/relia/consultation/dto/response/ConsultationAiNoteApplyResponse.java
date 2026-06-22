package com.linker.relia.consultation.dto.response;

import com.linker.relia.consultation.domain.stt.ConsultationAiNoteStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ConsultationAiNoteApplyResponse {
    private UUID aiNoteId;
    private ConsultationAiNoteStatus status;
    private LocalDateTime appliedAt;
    private ConsultationAiStructuredDraft structuredData;
    private ConsultationAiResolutionResponse resolutions;
    private List<String> warnings;
}
