package com.linker.relia.consultation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ConsultationSttSessionCompleteRequest {
    @NotBlank
    private String finalText;
}
