package com.linker.relia.commission.service;

import com.linker.relia.commission.domain.BranchCommissionMonthlyClosing;
import com.linker.relia.commission.domain.FpCommissionMonthlyClosing;
import com.linker.relia.commission.domain.IncomeCommissionMonthlyClosing;
import com.linker.relia.commission.dto.CommissionPaymentTypeSummaryResponse;
import com.linker.relia.commission.dto.FpCommissionMonthlyTrendResponse;
import com.linker.relia.commission.dto.FpCommissionSummaryRequest;
import com.linker.relia.commission.dto.FpCommissionSummaryResponse;
import com.linker.relia.commission.dto.InsuranceCompanyCommissionSummaryResponse;
import com.linker.relia.commission.dto.OrganizationCommissionMonthlyTrendQueryResult;
import com.linker.relia.commission.dto.OrganizationCommissionMonthlyTrendResponse;
import com.linker.relia.commission.dto.OrganizationCommissionSummaryResponse;
import com.linker.relia.commission.dto.OrganizationScopedClosingMonthRequest;
import com.linker.relia.commission.repository.BranchCommissionMonthlyClosingRepository;
import com.linker.relia.commission.repository.CommissionInsuranceCompanyQueryRepository;
import com.linker.relia.commission.repository.FpCommissionMonthlyClosingRepository;
import com.linker.relia.commission.repository.FpCommissionTrendQueryRepository;
import com.linker.relia.commission.repository.IncomeCommissionMonthlyClosingRepository;
import com.linker.relia.commission.repository.OrganizationCommissionTrendQueryRepository;
import com.linker.relia.common.access.AccessScope;
import com.linker.relia.organization.domain.Organization;
import com.linker.relia.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {
    private static final int ORGANIZATION_TREND_MONTH_COUNT = 5;

    private final CommissionAccessService commissionAccessService;
    private final FpCommissionMonthlyClosingRepository fpCommissionMonthlyClosingRepository;
    private final FpCommissionTrendQueryRepository fpCommissionTrendQueryRepository;
    private final BranchCommissionMonthlyClosingRepository branchCommissionMonthlyClosingRepository;
    private final IncomeCommissionMonthlyClosingRepository incomeCommissionMonthlyClosingRepository;
    private final CommissionInsuranceCompanyQueryRepository commissionInsuranceCompanyQueryRepository;
    private final OrganizationCommissionTrendQueryRepository organizationCommissionTrendQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public FpCommissionSummaryResponse getFpCommissionSummary(PrincipalDetails principalDetails, FpCommissionSummaryRequest request) {
        AccessScope accessScope = commissionAccessService.resolveAccessScope(principalDetails);
        String closingMonth = request.getClosingMonth().trim();
        FpCommissionMonthlyClosing current = fpCommissionMonthlyClosingRepository
                .findByFp_IdAndClosingMonth(accessScope.userId(), closingMonth)
                .orElse(null);

        if (current == null) {
            return FpCommissionSummaryResponse.empty(closingMonth);
        }

        String previousClosingMonth = YearMonth.parse(closingMonth).minusMonths(1).toString();
        FpCommissionMonthlyClosing previous = fpCommissionMonthlyClosingRepository
                .findByFp_IdAndClosingMonth(accessScope.userId(), previousClosingMonth)
                .orElse(null);

        return FpCommissionSummaryResponse.of(current, previous);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FpCommissionMonthlyTrendResponse> getFpCommissionTrend(PrincipalDetails principalDetails) {
        AccessScope accessScope = commissionAccessService.resolveAccessScope(principalDetails);
        YearMonth endMonth = YearMonth.now().minusMonths(1);
        YearMonth startMonth = endMonth.minusMonths(4);

        List<FpCommissionMonthlyTrendResponse> monthlyTrend = fpCommissionTrendQueryRepository
                .findFpTrendQueryResults(startMonth.toString(), endMonth.toString(), accessScope.userId())
                .stream()
                .map(FpCommissionMonthlyTrendResponse::from)
                .toList();

        Map<String, FpCommissionMonthlyTrendResponse> trendByMonth = new HashMap<>();
        for (FpCommissionMonthlyTrendResponse trend : monthlyTrend) {
            trendByMonth.put(trend.getClosingMonth(), trend);
        }

        return java.util.stream.IntStream.range(0, 5)
                .mapToObj(startMonth::plusMonths)
                .map(YearMonth::toString)
                .map(month -> trendByMonth.getOrDefault(month, FpCommissionMonthlyTrendResponse.empty(month)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationCommissionSummaryResponse getOrganizationCommissionSummary(PrincipalDetails principalDetails,
                                                                                  OrganizationScopedClosingMonthRequest request) {
        AccessScope accessScope = commissionAccessService.resolveAccessScope(principalDetails);
        String closingMonth = request.getClosingMonth().trim();
        String organizationCode = request.getOrganizationCode();

        if (accessScope.isBranchScope()) {
            commissionAccessService.validateOrganizationCodeFilter(
                    accessScope,
                    organizationCode,
                    principalDetails.getUser().getOrganization().getOrganizationCode()
            );
            return getBranchSummary(closingMonth, principalDetails.getUser().getOrganization());
        }

        if (organizationCode == null) {
            return getHqSummary(closingMonth);
        }

        Organization organization = commissionAccessService.resolveOrganization(organizationCode);
        return getBranchSummary(closingMonth, organization);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationCommissionMonthlyTrendResponse> getOrganizationCommissionTrend(PrincipalDetails principalDetails,
                                                                                           String organizationCode) {
        AccessScope accessScope = commissionAccessService.resolveAccessScope(principalDetails);
        YearMonth endMonth = YearMonth.now().minusMonths(1);

        if (accessScope.isBranchScope()) {
            Organization organization = principalDetails.getUser().getOrganization();
            commissionAccessService.validateOrganizationCodeFilter(
                    accessScope,
                    organizationCode,
                    organization.getOrganizationCode()
            );
            return getBranchTrend(endMonth, organization);
        }

        if (organizationCode == null || organizationCode.isBlank()) {
            return getHqTrend(endMonth);
        }

        Organization organization = commissionAccessService.resolveOrganization(organizationCode.trim());
        return getBranchTrend(endMonth, organization);
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionPaymentTypeSummaryResponse getCommissionPaymentTypeSummary(PrincipalDetails principalDetails,
                                                                                OrganizationScopedClosingMonthRequest request) {
        AccessScope accessScope = commissionAccessService.resolveAccessScope(principalDetails);
        String closingMonth = request.getClosingMonth().trim();
        String organizationCode = request.getOrganizationCode();

        if (accessScope.isOwnScope()) {
            commissionAccessService.validateOrganizationCodeFilter(
                    accessScope,
                    organizationCode,
                    principalDetails.getUser().getOrganization().getOrganizationCode()
            );
            return getFpPaymentTypeSummary(closingMonth, accessScope.userId());
        }

        if (accessScope.isBranchScope()) {
            commissionAccessService.validateOrganizationCodeFilter(
                    accessScope,
                    organizationCode,
                    principalDetails.getUser().getOrganization().getOrganizationCode()
            );
            return getBranchPaymentTypeSummary(closingMonth, principalDetails.getUser().getOrganization());
        }

        if (organizationCode == null || organizationCode.isBlank()) {
            return getHqPaymentTypeSummary(closingMonth);
        }

        Organization organization = commissionAccessService.resolveOrganization(organizationCode.trim());
        return getBranchPaymentTypeSummary(closingMonth, organization);
    }

    @Override
    @Transactional(readOnly = true)
    public InsuranceCompanyCommissionSummaryResponse getInsuranceCompanyCommissionSummary(PrincipalDetails principalDetails,
                                                                                          OrganizationScopedClosingMonthRequest request) {
        AccessScope accessScope = commissionAccessService.resolveAccessScope(principalDetails);
        String closingMonth = request.getClosingMonth().trim();
        String organizationCode = request.getOrganizationCode();

        if (accessScope.isOwnScope()) {
            commissionAccessService.validateOrganizationCodeFilter(accessScope, organizationCode, null);
            return InsuranceCompanyCommissionSummaryResponse.fpOf(
                    closingMonth,
                    commissionInsuranceCompanyQueryRepository.findFpSummaries(closingMonth, accessScope.userId())
            );
        }

        if (accessScope.isBranchScope()) {
            Organization organization = principalDetails.getUser().getOrganization();
            commissionAccessService.validateOrganizationCodeFilter(
                    accessScope,
                    organizationCode,
                    organization.getOrganizationCode()
            );
            return InsuranceCompanyCommissionSummaryResponse.branchOf(
                    closingMonth,
                    organization,
                    commissionInsuranceCompanyQueryRepository.findBranchSummaries(closingMonth, organization.getId())
            );
        }

        if (organizationCode == null || organizationCode.isBlank()) {
            return InsuranceCompanyCommissionSummaryResponse.hqOf(
                    closingMonth,
                    commissionInsuranceCompanyQueryRepository.findHqSummaries(closingMonth)
            );
        }

        Organization organization = commissionAccessService.resolveOrganization(organizationCode.trim());
        return InsuranceCompanyCommissionSummaryResponse.branchOf(
                closingMonth,
                organization,
                commissionInsuranceCompanyQueryRepository.findBranchSummaries(closingMonth, organization.getId())
        );
    }

    private OrganizationCommissionSummaryResponse getBranchSummary(String closingMonth, Organization organization) {
        BranchCommissionMonthlyClosing current = branchCommissionMonthlyClosingRepository
                .findByOrganization_IdAndClosingMonth(organization.getId(), closingMonth)
                .orElse(null);

        if (current == null) {
            return OrganizationCommissionSummaryResponse.emptyBranch(closingMonth, organization);
        }

        String previousClosingMonth = YearMonth.parse(closingMonth).minusMonths(1).toString();
        BranchCommissionMonthlyClosing previous = branchCommissionMonthlyClosingRepository
                .findByOrganization_IdAndClosingMonth(organization.getId(), previousClosingMonth)
                .orElse(null);

        return OrganizationCommissionSummaryResponse.branchOf(current, previous);
    }

    private List<OrganizationCommissionMonthlyTrendResponse> getBranchTrend(YearMonth endMonth, Organization organization) {
        YearMonth startMonth = endMonth.minusMonths(ORGANIZATION_TREND_MONTH_COUNT - 1L);

        List<OrganizationCommissionMonthlyTrendQueryResult> monthlyTrend = organizationCommissionTrendQueryRepository
                .findBranchTrendQueryResults(
                        startMonth.toString(),
                        endMonth.toString(),
                        organization.getId()
                );

        Map<String, OrganizationCommissionMonthlyTrendResponse> trendByMonth = new HashMap<>();
        for (OrganizationCommissionMonthlyTrendQueryResult trend : monthlyTrend) {
            trendByMonth.put(trend.getClosingMonth(), OrganizationCommissionMonthlyTrendResponse.from(trend));
        }

        List<OrganizationCommissionMonthlyTrendResponse> response = new ArrayList<>();
        for (int i = 0; i < ORGANIZATION_TREND_MONTH_COUNT; i++) {
            String month = startMonth.plusMonths(i).toString();
            response.add(trendByMonth.getOrDefault(
                    month,
                    OrganizationCommissionMonthlyTrendResponse.emptyBranch(
                            month,
                            organization.getId(),
                            organization.getOrganizationName()
                    )
            ));
        }
        return response;
    }

    private OrganizationCommissionSummaryResponse getHqSummary(String closingMonth) {
        IncomeCommissionMonthlyClosing current = incomeCommissionMonthlyClosingRepository
                .findByClosingMonth(closingMonth)
                .orElse(null);

        if (current == null) {
            return OrganizationCommissionSummaryResponse.emptyHq(closingMonth);
        }

        String previousClosingMonth = YearMonth.parse(closingMonth).minusMonths(1).toString();
        IncomeCommissionMonthlyClosing previous = incomeCommissionMonthlyClosingRepository
                .findByClosingMonth(previousClosingMonth)
                .orElse(null);

        return OrganizationCommissionSummaryResponse.hqOf(current, previous);
    }

    private List<OrganizationCommissionMonthlyTrendResponse> getHqTrend(YearMonth endMonth) {
        YearMonth startMonth = endMonth.minusMonths(ORGANIZATION_TREND_MONTH_COUNT - 1L);

        List<OrganizationCommissionMonthlyTrendQueryResult> monthlyTrend = organizationCommissionTrendQueryRepository
                .findHqTrendQueryResults(startMonth.toString(), endMonth.toString());

        Map<String, OrganizationCommissionMonthlyTrendResponse> trendByMonth = new HashMap<>();
        for (OrganizationCommissionMonthlyTrendQueryResult trend : monthlyTrend) {
            trendByMonth.put(trend.getClosingMonth(), OrganizationCommissionMonthlyTrendResponse.from(trend));
        }

        List<OrganizationCommissionMonthlyTrendResponse> response = new ArrayList<>();
        for (int i = 0; i < ORGANIZATION_TREND_MONTH_COUNT; i++) {
            String month = startMonth.plusMonths(i).toString();
            response.add(trendByMonth.getOrDefault(month, OrganizationCommissionMonthlyTrendResponse.emptyHq(month)));
        }
        return response;
    }

    private CommissionPaymentTypeSummaryResponse getFpPaymentTypeSummary(String closingMonth, UUID fpId) {
        FpCommissionMonthlyClosing current = fpCommissionMonthlyClosingRepository
                .findByFp_IdAndClosingMonth(fpId, closingMonth)
                .orElse(null);

        if (current == null) {
            return CommissionPaymentTypeSummaryResponse.emptyFp(closingMonth);
        }

        return CommissionPaymentTypeSummaryResponse.fpOf(current);
    }

    private CommissionPaymentTypeSummaryResponse getBranchPaymentTypeSummary(String closingMonth, Organization organization) {
        BranchCommissionMonthlyClosing current = branchCommissionMonthlyClosingRepository
                .findByOrganization_IdAndClosingMonth(organization.getId(), closingMonth)
                .orElse(null);

        if (current == null) {
            return CommissionPaymentTypeSummaryResponse.emptyBranch(closingMonth, organization);
        }

        return CommissionPaymentTypeSummaryResponse.branchOf(current);
    }

    private CommissionPaymentTypeSummaryResponse getHqPaymentTypeSummary(String closingMonth) {
        IncomeCommissionMonthlyClosing current = incomeCommissionMonthlyClosingRepository
                .findByClosingMonth(closingMonth)
                .orElse(null);

        if (current == null) {
            return CommissionPaymentTypeSummaryResponse.emptyHq(closingMonth);
        }

        return CommissionPaymentTypeSummaryResponse.hqOf(current);
    }
}
