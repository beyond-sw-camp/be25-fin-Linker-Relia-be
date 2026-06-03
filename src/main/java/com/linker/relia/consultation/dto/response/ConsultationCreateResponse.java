package com.linker.relia.consultation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class ConsultationCreateResponse {

    private UUID consultationId;
}