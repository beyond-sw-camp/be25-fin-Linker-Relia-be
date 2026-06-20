package com.linker.relia.consultation.service.stt;

import com.linker.relia.consultation.dto.response.ConsultationAiDraftResponse;
import com.linker.relia.consultation.dto.response.ConsultationAiNoteApplyResponse;

import java.util.UUID;

public interface ConsultationAiNoteService {
    void processSttCompleted(UUID sessionId, UUID fpId, String sttRawText);

    ConsultationAiDraftResponse getAiDraft(UUID sessionId, UUID fpId);

    ConsultationAiNoteApplyResponse applyAiDraft(UUID aiNoteId, UUID fpId);
}
