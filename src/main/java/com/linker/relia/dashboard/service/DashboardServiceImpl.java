package com.linker.relia.dashboard.service;

import com.linker.relia.auth.exception.AuthErrorCode;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.dashboard.dto.DashboardSummaryQueryResult;
import com.linker.relia.dashboard.dto.FpDashboardSummaryResponse;
import com.linker.relia.dashboard.repository.DashboardSummaryQueryRepository;
import com.linker.relia.security.principal.PrincipalDetails;
import com.linker.relia.user.domain.User;
import com.linker.relia.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final DashboardSummaryQueryRepository dashboardSummaryQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public FpDashboardSummaryResponse getFpSummary(PrincipalDetails principalDetails, LocalDate referenceDate) {
        User fp = principalDetails.getUser();
        validateFp(fp);

        LocalDate resolvedReferenceDate = referenceDate == null ? LocalDate.now() : referenceDate;
        YearMonth closingMonth = YearMonth.from(resolvedReferenceDate).minusMonths(1);
        YearMonth comparisonClosingMonth = closingMonth.minusMonths(1);

        /*
         * Business rule:
         * - The dashboard uses the previous closed month because the current month may not be closed yet.
         * - Diff values are calculated as previous closed month minus the month before that.
         * - Contract, retention, rank, customer, and handover values come from fp_monthly_performance_closing.
         * - Commission values come from fp_commission_monthly_closing.net_commission_amount.
         */
        DashboardSummaryQueryResult queryResult = dashboardSummaryQueryRepository.findFpSummary(
                fp.getId(),
                closingMonth.toString(),
                comparisonClosingMonth.toString()
        );

        return FpDashboardSummaryResponse.builder()
                .referenceDate(resolvedReferenceDate)
                .comparisonClosingMonth(closingMonth.toString())
                .newContractCount(queryResult.currentNewContractCount())
                .newContractDiff(queryResult.currentNewContractCount() - queryResult.previousNewContractCount())
                .retentionRate(queryResult.currentRetentionRate())
                .retentionRateDiff(queryResult.currentRetentionRate().subtract(queryResult.previousRetentionRate()))
                .branchRank(queryResult.currentBranchRank())
                .branchRankChange(calculateRankChange(queryResult.currentBranchRank(), queryResult.previousBranchRank()))
                .customerCount(queryResult.currentCustomerCount())
                .customerDiff(queryResult.currentCustomerCount() - queryResult.previousCustomerCount())
                .newHandoverCount(queryResult.currentNewHandoverCount())
                .handoverDiff(queryResult.currentNewHandoverCount() - queryResult.previousNewHandoverCount())
                .expectedCommissionAmount(queryResult.currentExpectedCommissionAmount())
                .commissionDiffAmount(queryResult.currentExpectedCommissionAmount()
                        .subtract(queryResult.previousClosedCommissionAmount()))
                .build();
    }

    private void validateFp(User user) {
        if (user.getUserRole() != UserRole.FP) {
            throw new BusinessException(AuthErrorCode.USER_FORBIDDEN);
        }

        if (user.getOrganization() == null) {
            throw new BusinessException(AuthErrorCode.INVALID_USER_STATE);
        }
    }

    private Integer calculateRankChange(Integer currentRank, Integer previousRank) {
        if (currentRank == null || previousRank == null) {
            return null;
        }

        return previousRank - currentRank;
    }
}
