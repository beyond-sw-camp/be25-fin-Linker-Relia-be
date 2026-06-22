package com.linker.relia.consultation.service.stt;

import com.linker.relia.consultation.dto.response.ConsultationAiStructuredDraft;

import java.util.List;

public record ConsultationAiDraftNormalizationResult(
        ConsultationAiStructuredDraft draft,
        List<String> warnings
) {
}
