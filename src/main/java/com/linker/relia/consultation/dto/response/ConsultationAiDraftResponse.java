package com.linker.relia.consultation.dto.response;

import com.linker.relia.consultation.domain.ConsultationType;
import com.linker.relia.consultation.domain.stt.ConsultationAiNoteStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ConsultationAiDraftResponse {
    private UUID aiNoteId;
    private UUID sessionId;
    private ConsultationType consultationType;
    private ConsultationAiNoteStatus draftStatus;
    private String sttRawText;
    private String summaryText;
    private ConsultationAiStructuredDraft structuredData;
    private ConsultationAiResolutionResponse resolutions;
    private String errorMessage;
}
