package com.linker.relia.commission.service;

import com.linker.relia.commission.domain.FpCommissionMonthlyClosing;
import com.linker.relia.commission.dto.FpCommissionSummaryRequest;
import com.linker.relia.commission.dto.FpCommissionSummaryResponse;
import com.linker.relia.commission.repository.FpCommissionMonthlyClosingRepository;
import com.linker.relia.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {
    private final FpCommissionMonthlyClosingRepository fpCommissionMonthlyClosingRepository;

    @Override
    @Transactional(readOnly = true)
    public FpCommissionSummaryResponse getFpCommissionSummary(PrincipalDetails principalDetails, FpCommissionSummaryRequest request) {
        String closingMonth = request.getClosingMonth().trim();
        FpCommissionMonthlyClosing current = fpCommissionMonthlyClosingRepository
                .findByFp_IdAndClosingMonth(principalDetails.getUser().getId(), closingMonth)
                .orElse(null);

        if (current == null) {
            return FpCommissionSummaryResponse.empty(closingMonth);
        }

        String previousClosingMonth = YearMonth.parse(closingMonth).minusMonths(1).toString();
        FpCommissionMonthlyClosing previous = fpCommissionMonthlyClosingRepository
                .findByFp_IdAndClosingMonth(principalDetails.getUser().getId(), previousClosingMonth)
                .orElse(null);

        return FpCommissionSummaryResponse.of(current, previous);
    }
}
