package com.linker.relia.consultation.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConsultationAiGenerationResult {
    private String summaryText;
    private ConsultationAiStructuredDraft structuredData;
}
