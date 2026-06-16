package com.linker.relia.handover.dto.request;

import java.util.UUID;

public record HandoverAssignRequest(
        UUID assignedFpId  // 직접 지정할 설계사 UUID
) {}
