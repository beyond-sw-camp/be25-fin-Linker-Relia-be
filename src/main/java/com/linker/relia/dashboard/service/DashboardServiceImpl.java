package com.linker.relia.dashboard.service;

import com.linker.relia.auth.exception.AuthErrorCode;
import com.linker.relia.common.access.AccessScope;
import com.linker.relia.common.access.AccessScopeResolver;
import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.commission.domain.IncomeCommissionMonthlyClosing;
import com.linker.relia.commission.dto.FpCommissionMonthlyTrendQueryResult;
import com.linker.relia.commission.repository.IncomeCommissionMonthlyClosingRepository;
import com.linker.relia.commission.repository.custom.FpCommissionTrendQueryRepository;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.common.exception.CommonErrorCode;
import com.linker.relia.dashboard.dto.DashboardClosingMonthOptionResponse;
import com.linker.relia.dashboard.dto.DashboardContractDistributionQueryResult;
import com.linker.relia.dashboard.dto.DashboardFpRankingItemResponse;
import com.linker.relia.dashboard.dto.DashboardFpRankingRequest;
import com.linker.relia.dashboard.dto.DashboardFpRankingResponse;
import com.linker.relia.dashboard.dto.DashboardFpRankOrder;
import com.linker.relia.dashboard.dto.DashboardContractStatusQueryResult;
import com.linker.relia.dashboard.dto.DashboardKpiQueryResult;
import com.linker.relia.dashboard.dto.DashboardKpiRequest;
import com.linker.relia.dashboard.dto.DashboardMonthlyContractCustomerTrendQueryResult;
import com.linker.relia.dashboard.dto.DashboardOrganizationContractDistributionRequest;
import com.linker.relia.dashboard.dto.DashboardSummaryQueryResult;
import com.linker.relia.dashboard.dto.FpDashboardContractDistributionResponse;
import com.linker.relia.dashboard.dto.FpDashboardContractStatusResponse;
import com.linker.relia.dashboard.dto.FpDashboardMonthlyCommissionTrendResponse;
import com.linker.relia.dashboard.dto.FpDashboardMonthlyContractCustomerTrendResponse;
import com.linker.relia.dashboard.dto.FpDashboardSummaryResponse;
import com.linker.relia.dashboard.dto.OrganizationDashboardContractDistributionResponse;
import com.linker.relia.dashboard.dto.OrganizationDashboardKpiResponse;
import com.linker.relia.dashboard.repository.DashboardContractDistributionQueryRepository;
import com.linker.relia.dashboard.repository.DashboardFpRankingQueryRepository;
import com.linker.relia.dashboard.repository.DashboardContractStatusQueryRepository;
import com.linker.relia.dashboard.repository.DashboardKpiQueryRepository;
import com.linker.relia.dashboard.repository.DashboardMonthlyContractCustomerTrendQueryRepository;
import com.linker.relia.dashboard.repository.DashboardSummaryQueryRepository;
import com.linker.relia.organization.domain.Organization;
import com.linker.relia.organization.domain.OrganizationStatus;
import com.linker.relia.organization.domain.OrganizationType;
import com.linker.relia.organization.exception.OrganizationErrorCode;
import com.linker.relia.organization.repository.OrganizationRepository;
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
    private final AccessScopeResolver accessScopeResolver;
    private final OrganizationRepository organizationRepository;
    private final DashboardSummaryQueryRepository dashboardSummaryQueryRepository;
    private final DashboardContractStatusQueryRepository dashboardContractStatusQueryRepository;
    private final DashboardContractDistributionQueryRepository dashboardContractDistributionQueryRepository;
    private final DashboardFpRankingQueryRepository dashboardFpRankingQueryRepository;
    private final DashboardKpiQueryRepository dashboardKpiQueryRepository;
    private final DashboardMonthlyContractCustomerTrendQueryRepository dashboardMonthlyContractCustomerTrendQueryRepository;
    private final FpCommissionTrendQueryRepository fpCommissionTrendQueryRepository;
    private final IncomeCommissionMonthlyClosingRepository incomeCommissionMonthlyClosingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DashboardClosingMonthOptionResponse> getClosingMonthOptions() {
        return incomeCommissionMonthlyClosingRepository.findAllByOrderByClosingMonthDesc()
                .stream()
                .map(this::toClosingMonthOption)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationDashboardKpiResponse getOrganizationKpi(
            PrincipalDetails principalDetails,
            DashboardKpiRequest request
    ) {
        AccessScope accessScope = accessScopeResolver.resolve(principalDetails);
        validateManagerScope(accessScope);

        String closingMonth = normalizeRequiredClosingMonth(request.getClosingMonth());
        String comparisonClosingMonth = YearMonth.parse(closingMonth).minusMonths(1).toString();
        Organization targetOrganization = resolveTargetOrganization(accessScope, principalDetails, request.getOrganizationCode());

        DashboardKpiQueryResult current = targetOrganization == null
                ? dashboardKpiQueryRepository.findHqKpi(closingMonth)
                : dashboardKpiQueryRepository.findBranchKpi(targetOrganization.getId(), closingMonth);

        DashboardKpiQueryResult previous = targetOrganization == null
                ? dashboardKpiQueryRepository.findHqKpi(comparisonClosingMonth)
                : dashboardKpiQueryRepository.findBranchKpi(targetOrganization.getId(), comparisonClosingMonth);

        return OrganizationDashboardKpiResponse.builder()
                .closingMonth(closingMonth)
                .comparisonClosingMonth(comparisonClosingMonth)
                .organizationCode(targetOrganization == null ? null : targetOrganization.getOrganizationCode())
                .organizationName(targetOrganization == null ? null : targetOrganization.getOrganizationName())
                .fpCount(current.fpCount())
                .fpCountDiff(current.fpCount() - previous.fpCount())
                .customerCount(current.customerCount())
                .customerCountDiff(current.customerCount() - previous.customerCount())
                .totalContractCount(current.totalContractCount())
                .totalContractCountDiff(current.totalContractCount() - previous.totalContractCount())
                .interestCustomerCount(current.interestCustomerCount())
                .interestCustomerCountDiff(current.interestCustomerCount() - previous.interestCustomerCount())
                .interestCustomerRate(calculateInterestCustomerRate(current.interestCustomerCount(), current.customerCount()))
                .contractSuccessRate(current.contractSuccessRate())
                .contractSuccessRateDiff(current.contractSuccessRate().subtract(previous.contractSuccessRate()))
                .retentionRate(current.retentionRate())
                .retentionRateDiff(current.retentionRate().subtract(previous.retentionRate()))
                .terminatedContractCount(current.terminatedContractCount())
                .terminatedContractCountDiff(current.terminatedContractCount() - previous.terminatedContractCount())
                .netIncomeCommissionAmount(current.netIncomeCommissionAmount())
                .netIncomeCommissionDiffAmount(current.netIncomeCommissionAmount().subtract(previous.netIncomeCommissionAmount()))
                .totalPaymentCommissionAmount(current.totalPaymentCommissionAmount())
                .totalPaymentCommissionDiffAmount(current.totalPaymentCommissionAmount().subtract(previous.totalPaymentCommissionAmount()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationDashboardContractDistributionResponse getOrganizationContractDistribution(
            PrincipalDetails principalDetails,
            DashboardOrganizationContractDistributionRequest request
    ) {
        AccessScope accessScope = accessScopeResolver.resolve(principalDetails);
        validateManagerScope(accessScope);

        String closingMonth = normalizeRequiredClosingMonth(request.getClosingMonth());
        Organization targetOrganization = resolveTargetOrganization(accessScope, principalDetails, request.getOrganizationCode());

        List<DashboardContractDistributionQueryResult> insuranceCompanyResults = targetOrganization == null
                ? dashboardContractDistributionQueryRepository.summarizeAllInsuranceCompanyContractCounts(closingMonth)
                : dashboardContractDistributionQueryRepository.summarizeInsuranceCompanyContractCountsByOrganization(
                targetOrganization.getId(),
                closingMonth
        );
        List<DashboardContractDistributionQueryResult> insuranceCategoryResults = targetOrganization == null
                ? dashboardContractDistributionQueryRepository.summarizeAllInsuranceCategoryContractCounts(closingMonth)
                : dashboardContractDistributionQueryRepository.summarizeInsuranceCategoryContractCountsByOrganization(
                targetOrganization.getId(),
                closingMonth
        );

        long totalContractCount = calculateTotalContractCount(insuranceCompanyResults);

        return OrganizationDashboardContractDistributionResponse.builder()
                .closingMonth(closingMonth)
                .organizationCode(targetOrganization == null ? null : targetOrganization.getOrganizationCode())
                .organizationName(targetOrganization == null ? null : targetOrganization.getOrganizationName())
                .totalContractCount(totalContractCount)
                .insuranceCompanies(
                        insuranceCompanyResults.stream()
                                .map(result -> toOrganizationInsuranceCompanyDistributionItem(result, totalContractCount))
                                .toList()
                )
                .insuranceCategories(
                        insuranceCategoryResults.stream()
                                .map(result -> toOrganizationInsuranceCategoryDistributionItem(result, totalContractCount))
                                .toList()
                )
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardFpRankingResponse getOrganizationFpRankings(
            PrincipalDetails principalDetails,
            DashboardFpRankingRequest request
    ) {
        AccessScope accessScope = accessScopeResolver.resolve(principalDetails);
        validateManagerScope(accessScope);
        validateRankingPageSize(request.getSize());

        String closingMonth = resolveRankingClosingMonth(request.getClosingMonth());
        Organization targetOrganization =
                resolveTargetOrganization(accessScope, principalDetails, request.getOrganizationCode());
        DashboardFpRankOrder rankOrder = request.getRankOrder() == null
                ? DashboardFpRankOrder.TOP
                : request.getRankOrder();

        PageResponse<DashboardFpRankingItemResponse> rankings =
                PageResponse.from(dashboardFpRankingQueryRepository.findFpRankings(
                        closingMonth,
                        targetOrganization == null ? null : targetOrganization.getId(),
                        rankOrder,
                        request.toPageable()
                ));

        return DashboardFpRankingResponse.builder()
                .closingMonth(closingMonth)
                .organizationCode(targetOrganization == null ? null : targetOrganization.getOrganizationCode())
                .organizationName(targetOrganization == null ? null : targetOrganization.getOrganizationName())
                .rankOrder(rankOrder)
                .rankings(rankings)
                .build();
    }

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

    private void validateManagerScope(AccessScope accessScope) {
        if (accessScope.isOwnScope()) {
            throw new BusinessException(AuthErrorCode.USER_FORBIDDEN);
        }
    }

    private String normalizeRequiredClosingMonth(String closingMonth) {
        if (closingMonth == null || closingMonth.isBlank()) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "closingMonth는 필수입니다.");
        }

        try {
            return YearMonth.parse(closingMonth.trim()).toString();
        } catch (Exception exception) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "closingMonth는 YYYY-MM 형식이어야 합니다.");
        }
    }

    private String resolveRankingClosingMonth(String closingMonth) {
        if (closingMonth != null && !closingMonth.isBlank()) {
            try {
                return YearMonth.parse(closingMonth.trim()).toString();
            } catch (Exception exception) {
                throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "closingMonth는 YYYY-MM 형식이어야 합니다.");
            }
        }

        return dashboardFpRankingQueryRepository.findLatestClosingMonth()
                .orElseThrow(() -> new BusinessException(
                        CommonErrorCode.INVALID_REQUEST,
                        "조회할 수 있는 설계사 실적 마감 데이터가 없습니다."
                ));
    }

    private void validateRankingPageSize(Integer size) {
        if (size != null && size > 100) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "size는 100 이하여야 합니다.");
        }
    }

    private Organization resolveTargetOrganization(
            AccessScope accessScope,
            PrincipalDetails principalDetails,
            String organizationCode
    ) {
        if (accessScope.isBranchScope()) {
            Organization ownOrganization = principalDetails.getUser().getOrganization();
            if (ownOrganization == null) {
                throw new BusinessException(AuthErrorCode.INVALID_USER_STATE);
            }

            if (organizationCode == null || organizationCode.isBlank()) {
                return ownOrganization;
            }

            if (!organizationCode.trim().equals(ownOrganization.getOrganizationCode())) {
                throw new BusinessException(AuthErrorCode.USER_FORBIDDEN);
            }
            return ownOrganization;
        }

        if (organizationCode == null || organizationCode.isBlank()) {
            return null;
        }

        Organization organization = organizationRepository.findByOrganizationCode(organizationCode.trim())
                .filter(found -> found.getDeletedAt() == null)
                .filter(found -> found.getOrganizationType() == OrganizationType.BRANCH)
                .filter(found -> found.getOrganizationStatus() == OrganizationStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));

        return organization;
    }

    private BigDecimal calculateInterestCustomerRate(long interestCustomerCount, long customerCount) {
        if (customerCount == 0L) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(interestCustomerCount)
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(customerCount), 2, java.math.RoundingMode.HALF_UP);
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

    private OrganizationDashboardContractDistributionResponse.InsuranceCompanyDistributionItem
    toOrganizationInsuranceCompanyDistributionItem(
            DashboardContractDistributionQueryResult queryResult,
            long totalContractCount
    ) {
        return OrganizationDashboardContractDistributionResponse.InsuranceCompanyDistributionItem.builder()
                .insuranceCompanyId(queryResult.id())
                .insuranceCompanyName(queryResult.name())
                .contractCount(queryResult.contractCount())
                .contractRatio(calculateContractRatio(queryResult.contractCount(), totalContractCount))
                .build();
    }

    private OrganizationDashboardContractDistributionResponse.InsuranceCategoryDistributionItem
    toOrganizationInsuranceCategoryDistributionItem(
            DashboardContractDistributionQueryResult queryResult,
            long totalContractCount
    ) {
        return OrganizationDashboardContractDistributionResponse.InsuranceCategoryDistributionItem.builder()
                .insuranceCategoryId(queryResult.id())
                .insuranceCategoryName(queryResult.name())
                .contractCount(queryResult.contractCount())
                .contractRatio(calculateContractRatio(queryResult.contractCount(), totalContractCount))
                .build();
    }

    private long calculateTotalContractCount(List<DashboardContractDistributionQueryResult> queryResults) {
        return queryResults.stream()
                .mapToLong(DashboardContractDistributionQueryResult::contractCount)
                .sum();
    }

    private BigDecimal calculateContractRatio(long contractCount, long totalContractCount) {
        if (totalContractCount == 0L) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(contractCount)
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(totalContractCount), 2, java.math.RoundingMode.HALF_UP);
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

    private DashboardClosingMonthOptionResponse toClosingMonthOption(IncomeCommissionMonthlyClosing closing) {
        return DashboardClosingMonthOptionResponse.builder()
                .closingMonth(closing.getClosingMonth())
                .build();
    }
}
