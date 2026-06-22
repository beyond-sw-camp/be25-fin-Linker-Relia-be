package com.linker.relia.consultation.service.stt;

import com.linker.relia.consultation.dto.response.ConsultationAiResolutionResponse;
import com.linker.relia.consultation.dto.response.ConsultationAiStructuredDraft;

public record ConsultationAiDraftResolutionResult(
        ConsultationAiStructuredDraft draft,
        ConsultationAiResolutionResponse resolution
) {
}
