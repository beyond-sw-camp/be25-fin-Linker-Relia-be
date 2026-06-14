package com.linker.relia.commission.service;

import com.linker.relia.commission.dto.FpCommissionSummaryRequest;
import com.linker.relia.commission.dto.FpCommissionSummaryResponse;
import com.linker.relia.security.principal.PrincipalDetails;

public interface CommissionService {
    FpCommissionSummaryResponse getFpCommissionSummary(PrincipalDetails principalDetails, FpCommissionSummaryRequest request);
}
