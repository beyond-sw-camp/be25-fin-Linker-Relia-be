package com.linker.relia.handover.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record HandoverAssignRequest(
        @NotNull(message = "지정할 설계사 ID는 필수입니다.")
        UUID assignedFpId  // 직접 지정할 설계사 UUID
) {}
