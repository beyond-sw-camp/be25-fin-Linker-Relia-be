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

import java.math.BigDecimal;
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
        YearMonth currentMonth = YearMonth.from(resolvedReferenceDate);
        YearMonth comparisonClosingMonth = currentMonth.minusMonths(1);

        /*
         * Business 기준:
         * - 이번 달 실시간 값: referenceDate가 속한 월의 1일부터 referenceDate까지 운영 테이블에서 집계한다.
         * - 전월 대비 값: 이번 달 실시간 값에서 comparisonClosingMonth의 마감 데이터를 뺀다.
         * - 예상 수수료/현재 지점 순위: payment_commission_records의 현재 월 레코드를 실시간 합산한다.
         * - 전월 수수료/전월 지점 순위: fp_commission_monthly_closing의 net_commission_amount를 사용한다.
         */
        DashboardSummaryQueryResult queryResult = dashboardSummaryQueryRepository.findFpSummary(
                fp.getId(),
                fp.getOrganization().getId(),
                currentMonth.atDay(1),
                resolvedReferenceDate,
                currentMonth.toString(),
                comparisonClosingMonth.toString()
        );

        return FpDashboardSummaryResponse.builder()
                .referenceDate(resolvedReferenceDate)
                .comparisonClosingMonth(comparisonClosingMonth.toString())
                .newContractCount(queryResult.currentNewContractCount())
                .newContractDiff(queryResult.currentNewContractCount() - queryResult.previousNewContractCount())
                .retentionRate(queryResult.currentRetentionRate())
                .retentionRateDiff(queryResult.currentRetentionRate().subtract(queryResult.previousRetentionRate()))
                .branchRank(queryResult.currentBranchRank())
                .branchRankChange(calculateRankChange(queryResult.currentBranchRank(), queryResult.previousBranchRank()))
                .customerCount(queryResult.currentCustomerCount())
                .customerDiff(queryResult.customerNetIncreaseCount())
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
