package com.linker.relia.commission.service;

import com.linker.relia.commission.domain.BranchCommissionMonthlyClosing;
import com.linker.relia.commission.domain.FpCommissionMonthlyClosing;
import com.linker.relia.commission.domain.IncomeCommissionMonthlyClosing;
import com.linker.relia.commission.dto.CommissionPaymentTypeSummaryResponse;
import com.linker.relia.commission.dto.FpCommissionSummaryRequest;
import com.linker.relia.commission.dto.FpCommissionSummaryResponse;
import com.linker.relia.commission.dto.InsuranceCompanyCommissionSummaryResponse;
import com.linker.relia.commission.dto.OrganizationCommissionSummaryResponse;
import com.linker.relia.commission.dto.OrganizationScopedClosingMonthRequest;
import com.linker.relia.commission.repository.BranchCommissionMonthlyClosingRepository;
import com.linker.relia.commission.repository.CommissionInsuranceCompanyQueryRepository;
import com.linker.relia.commission.repository.FpCommissionMonthlyClosingRepository;
import com.linker.relia.commission.repository.IncomeCommissionMonthlyClosingRepository;
import com.linker.relia.common.access.AccessScope;
import com.linker.relia.organization.domain.Organization;
import com.linker.relia.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {
    private final CommissionAccessService commissionAccessService;
    private final FpCommissionMonthlyClosingRepository fpCommissionMonthlyClosingRepository;
    private final BranchCommissionMonthlyClosingRepository branchCommissionMonthlyClosingRepository;
    private final IncomeCommissionMonthlyClosingRepository incomeCommissionMonthlyClosingRepository;
    private final CommissionInsuranceCompanyQueryRepository commissionInsuranceCompanyQueryRepository;

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
    public OrganizationCommissionSummaryResponse getOrganizationCommissionSummary(PrincipalDetails principalDetails,
                                                                                  OrganizationScopedClosingMonthRequest request) {
        AccessScope accessScope = commissionAccessService.resolveAccessScope(principalDetails);
        String closingMonth = request.getClosingMonth().trim();
        String organizationCode = request.getOrganizationCode();

        if (accessScope.isBranchScope()) {
            commissionAccessService.validateOrganizationCodeFilter(accessScope, organizationCode,
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
