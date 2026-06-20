package com.linker.relia.consultation.service.stt;

import com.linker.relia.consultation.domain.stt.ConsultationSttSession;
import com.linker.relia.consultation.dto.response.ConsultationAiGenerationResult;

public interface ConsultationAiDraftGenerator {
    ConsultationAiGenerationResult generate(ConsultationSttSession session, String sttRawText);
}
