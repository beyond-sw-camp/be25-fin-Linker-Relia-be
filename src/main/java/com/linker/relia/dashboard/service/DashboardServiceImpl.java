package com.linker.relia.dashboard.service;

import com.linker.relia.auth.exception.AuthErrorCode;
import com.linker.relia.commission.dto.FpCommissionMonthlyTrendQueryResult;
import com.linker.relia.commission.repository.custom.FpCommissionTrendQueryRepository;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.dashboard.dto.DashboardContractDistributionQueryResult;
import com.linker.relia.dashboard.dto.DashboardContractStatusQueryResult;
import com.linker.relia.dashboard.dto.DashboardMonthlyContractCustomerTrendQueryResult;
import com.linker.relia.dashboard.dto.DashboardSummaryQueryResult;
import com.linker.relia.dashboard.dto.FpDashboardContractDistributionResponse;
import com.linker.relia.dashboard.dto.FpDashboardContractStatusResponse;
import com.linker.relia.dashboard.dto.FpDashboardMonthlyCommissionTrendResponse;
import com.linker.relia.dashboard.dto.FpDashboardMonthlyContractCustomerTrendResponse;
import com.linker.relia.dashboard.dto.FpDashboardSummaryResponse;
import com.linker.relia.dashboard.repository.DashboardContractDistributionQueryRepository;
import com.linker.relia.dashboard.repository.DashboardContractStatusQueryRepository;
import com.linker.relia.dashboard.repository.DashboardMonthlyContractCustomerTrendQueryRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final DashboardSummaryQueryRepository dashboardSummaryQueryRepository;
    private final DashboardContractStatusQueryRepository dashboardContractStatusQueryRepository;
    private final DashboardContractDistributionQueryRepository dashboardContractDistributionQueryRepository;
    private final DashboardMonthlyContractCustomerTrendQueryRepository dashboardMonthlyContractCustomerTrendQueryRepository;
    private final FpCommissionTrendQueryRepository fpCommissionTrendQueryRepository;

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

    @Override
    @Transactional(readOnly = true)
    public FpDashboardContractStatusResponse getFpContractStatus(PrincipalDetails principalDetails,
                                                                 LocalDate referenceDate) {
        User fp = principalDetails.getUser();
        validateFp(fp);

        LocalDate resolvedReferenceDate = referenceDate == null ? LocalDate.now() : referenceDate;
        YearMonth closingMonth = YearMonth.from(resolvedReferenceDate).minusMonths(1);

        DashboardContractStatusQueryResult queryResult =
                dashboardContractStatusQueryRepository.summarizeContractStatuses(
                        fp.getId(),
                        closingMonth.toString()
                );

        return FpDashboardContractStatusResponse.builder()
                .referenceDate(resolvedReferenceDate)
                .closingMonth(closingMonth.toString())
                .totalContractCount(queryResult.totalContractCount())
                .maintenanceContractCount(queryResult.maintenanceContractCount())
                .lapsedContractCount(queryResult.lapsedContractCount())
                .terminatedContractCount(queryResult.terminatedContractCount())
                .completedContractCount(queryResult.completedContractCount())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FpDashboardContractDistributionResponse getFpContractDistribution(
            PrincipalDetails principalDetails,
            LocalDate referenceDate
    ) {
        User fp = principalDetails.getUser();
        validateFp(fp);

        LocalDate resolvedReferenceDate = referenceDate == null ? LocalDate.now() : referenceDate;
        YearMonth closingMonth = YearMonth.from(resolvedReferenceDate).minusMonths(1);

        List<DashboardContractDistributionQueryResult> insuranceCompanyResults =
                dashboardContractDistributionQueryRepository.summarizeInsuranceCompanyContractCounts(
                        fp.getId(),
                        closingMonth.toString()
                );
        List<DashboardContractDistributionQueryResult> insuranceCategoryResults =
                dashboardContractDistributionQueryRepository.summarizeInsuranceCategoryContractCounts(
                        fp.getId(),
                        closingMonth.toString()
                );

        List<FpDashboardContractDistributionResponse.InsuranceCompanyContractCountItem> insuranceCompanyItems =
                insuranceCompanyResults.stream()
                        .map(this::toInsuranceCompanyContractCountItem)
                        .toList();
        List<FpDashboardContractDistributionResponse.InsuranceCategoryContractCountItem> insuranceCategoryItems =
                insuranceCategoryResults.stream()
                        .map(this::toInsuranceCategoryContractCountItem)
                        .toList();

        return FpDashboardContractDistributionResponse.builder()
                .referenceDate(resolvedReferenceDate)
                .closingMonth(closingMonth.toString())
                .totalContractCount(calculateTotalContractCount(insuranceCompanyResults))
                .insuranceCompanies(insuranceCompanyItems)
                .insuranceCategories(insuranceCategoryItems)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FpDashboardMonthlyContractCustomerTrendResponse getFpMonthlyContractCustomerTrend(
            PrincipalDetails principalDetails,
            LocalDate referenceDate
    ) {
        User fp = principalDetails.getUser();
        validateFp(fp);

        LocalDate resolvedReferenceDate = referenceDate == null ? LocalDate.now() : referenceDate;
        YearMonth endMonth = YearMonth.from(resolvedReferenceDate).minusMonths(1);
        YearMonth startMonth = endMonth.minusMonths(5);

        List<DashboardMonthlyContractCustomerTrendQueryResult> queryResults =
                dashboardMonthlyContractCustomerTrendQueryRepository.findMonthlyContractCustomerTrends(
                        fp.getId(),
                        startMonth.toString(),
                        endMonth.toString()
                );
        Map<String, DashboardMonthlyContractCustomerTrendQueryResult> trendByMonth =
                mapMonthlyContractCustomerTrends(queryResults);

        return FpDashboardMonthlyContractCustomerTrendResponse.builder()
                .referenceDate(resolvedReferenceDate)
                .startMonth(startMonth.toString())
                .endMonth(endMonth.toString())
                .monthlyTrends(buildMonthlyContractCustomerTrendItems(startMonth, trendByMonth))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FpDashboardMonthlyCommissionTrendResponse getFpMonthlyCommissionTrend(
            PrincipalDetails principalDetails,
            LocalDate referenceDate
    ) {
        User fp = principalDetails.getUser();
        validateFp(fp);

        LocalDate resolvedReferenceDate = referenceDate == null ? LocalDate.now() : referenceDate;
        YearMonth endMonth = YearMonth.from(resolvedReferenceDate).minusMonths(1);
        YearMonth startMonth = endMonth.minusMonths(5);

        List<FpCommissionMonthlyTrendQueryResult> queryResults = fpCommissionTrendQueryRepository.findFpTrendQueryResults(
                startMonth.toString(),
                endMonth.toString(),
                fp.getId()
        );
        Map<String, FpCommissionMonthlyTrendQueryResult> trendByMonth = mapMonthlyCommissionTrends(queryResults);

        return FpDashboardMonthlyCommissionTrendResponse.builder()
                .referenceDate(resolvedReferenceDate)
                .startMonth(startMonth.toString())
                .endMonth(endMonth.toString())
                .monthlyTrends(buildMonthlyCommissionTrendItems(startMonth, trendByMonth))
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

    private FpDashboardContractDistributionResponse.InsuranceCompanyContractCountItem
    toInsuranceCompanyContractCountItem(DashboardContractDistributionQueryResult queryResult) {
        return FpDashboardContractDistributionResponse.InsuranceCompanyContractCountItem.builder()
                .insuranceCompanyId(queryResult.id())
                .insuranceCompanyName(queryResult.name())
                .contractCount(queryResult.contractCount())
                .build();
    }

    private FpDashboardContractDistributionResponse.InsuranceCategoryContractCountItem
    toInsuranceCategoryContractCountItem(DashboardContractDistributionQueryResult queryResult) {
        return FpDashboardContractDistributionResponse.InsuranceCategoryContractCountItem.builder()
                .insuranceCategoryId(queryResult.id())
                .insuranceCategoryName(queryResult.name())
                .contractCount(queryResult.contractCount())
                .build();
    }

    private long calculateTotalContractCount(List<DashboardContractDistributionQueryResult> queryResults) {
        return queryResults.stream()
                .mapToLong(DashboardContractDistributionQueryResult::contractCount)
                .sum();
    }

    private Map<String, DashboardMonthlyContractCustomerTrendQueryResult> mapMonthlyContractCustomerTrends(
            List<DashboardMonthlyContractCustomerTrendQueryResult> queryResults
    ) {
        Map<String, DashboardMonthlyContractCustomerTrendQueryResult> trendByMonth = new HashMap<>();
        for (DashboardMonthlyContractCustomerTrendQueryResult queryResult : queryResults) {
            trendByMonth.put(queryResult.closingMonth(), queryResult);
        }
        return trendByMonth;
    }

    private List<FpDashboardMonthlyContractCustomerTrendResponse.MonthlyContractCustomerTrendItem>
    buildMonthlyContractCustomerTrendItems(
            YearMonth startMonth,
            Map<String, DashboardMonthlyContractCustomerTrendQueryResult> trendByMonth
    ) {
        return java.util.stream.IntStream.range(0, 6)
                .mapToObj(index -> {
                    String month = startMonth.plusMonths(index).toString();
                    DashboardMonthlyContractCustomerTrendQueryResult queryResult = trendByMonth.get(month);
                    return FpDashboardMonthlyContractCustomerTrendResponse.MonthlyContractCustomerTrendItem.builder()
                            .month(month)
                            .newContractCount(queryResult == null ? 0 : queryResult.newContractCount())
                            .customerCount(queryResult == null ? 0 : queryResult.customerCount())
                            .build();
                })
                .toList();
    }

    private Map<String, FpCommissionMonthlyTrendQueryResult> mapMonthlyCommissionTrends(
            List<FpCommissionMonthlyTrendQueryResult> queryResults
    ) {
        Map<String, FpCommissionMonthlyTrendQueryResult> trendByMonth = new HashMap<>();
        for (FpCommissionMonthlyTrendQueryResult queryResult : queryResults) {
            trendByMonth.put(queryResult.getClosingMonth(), queryResult);
        }
        return trendByMonth;
    }

    private List<FpDashboardMonthlyCommissionTrendResponse.MonthlyCommissionTrendItem> buildMonthlyCommissionTrendItems(
            YearMonth startMonth,
            Map<String, FpCommissionMonthlyTrendQueryResult> trendByMonth
    ) {
        return java.util.stream.IntStream.range(0, 6)
                .mapToObj(index -> {
                    String month = startMonth.plusMonths(index).toString();
                    FpCommissionMonthlyTrendQueryResult queryResult = trendByMonth.get(month);
                    return FpDashboardMonthlyCommissionTrendResponse.MonthlyCommissionTrendItem.builder()
                            .month(month)
                            .netCommissionAmount(queryResult == null ? BigDecimal.ZERO : queryResult.getNetCommissionAmount())
                            .build();
                })
                .toList();
    }
}
