package com.linker.relia.consultation.dto.request;

import com.linker.relia.consultation.domain.ConsultationType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ConsultationSttSessionStartRequest {
    private UUID customerId;

    @NotNull
    private ConsultationType consultationType;
}
