package com.linker.relia.handover.dto.request;

import com.linker.relia.handover.domain.RequestType;

import java.util.UUID;

public record HandoverCreateRequest(
        UUID customerId,
        RequestType requestType
) {}