package com.linker.relia.consultation.service.ai;

import com.linker.relia.customer.dto.CustomerAiBriefingResponse;
import com.linker.relia.security.principal.PrincipalDetails;

import java.util.UUID;

public interface ConsultationAiBriefingService {
    CustomerAiBriefingResponse generateAiBriefing(PrincipalDetails principalDetails, UUID customerId);

    CustomerAiBriefingResponse getLatestAiBriefing(PrincipalDetails principalDetails, UUID customerId);
}