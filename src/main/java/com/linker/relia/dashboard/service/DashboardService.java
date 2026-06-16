package com.linker.relia.dashboard.service;

import com.linker.relia.dashboard.dto.FpDashboardSummaryResponse;
import com.linker.relia.security.principal.PrincipalDetails;

import java.time.LocalDate;

public interface DashboardService {
    FpDashboardSummaryResponse getFpSummary(PrincipalDetails principalDetails, LocalDate referenceDate);
}
