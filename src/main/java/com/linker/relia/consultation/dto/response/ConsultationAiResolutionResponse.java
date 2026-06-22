package com.linker.relia.consultation.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ConsultationAiResolutionResponse {
    private boolean hasPendingResolution;
    private List<FieldResolution> fields;

    public static ConsultationAiResolutionResponse empty() {
        return ConsultationAiResolutionResponse.builder()
                .hasPendingResolution(false)
                .fields(List.of())
                .build();
    }

    @Getter
    @Builder
    public static class FieldResolution {
        private String fieldPath;
        private String rawValue;
        private String status;
        private String message;
        private List<Candidate> candidates;
    }

    @Getter
    @Builder
    public static class Candidate {
        private String id;
        private String code;
        private String label;
        private String subLabel;
    }
}
