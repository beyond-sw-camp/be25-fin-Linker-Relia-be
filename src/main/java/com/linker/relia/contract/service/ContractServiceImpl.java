package com.linker.relia.contract.service;

import com.linker.relia.auth.exception.AuthErrorCode;
import com.linker.relia.common.access.AccessScope;
import com.linker.relia.common.access.AccessScopeResolver;
import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.contract.dto.ContractDetailQueryResult;
import com.linker.relia.contract.dto.ContractDetailResponse;
import com.linker.relia.contract.dto.ContractListItemResponse;
import com.linker.relia.contract.dto.ContractListRequest;
import com.linker.relia.contract.dto.ContractSummaryRequest;
import com.linker.relia.contract.dto.ContractSummaryResponse;
import com.linker.relia.contract.dto.InsuranceCompanyContractStatusResponse;
import com.linker.relia.contract.exception.ContractErrorCode;
import com.linker.relia.contract.repository.ContractRepository;
import com.linker.relia.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {
    private final ContractRepository contractRepository;
    private final AccessScopeResolver accessScopeResolver;

    @Override
    @Transactional(readOnly = true)
    public ContractSummaryResponse getContractSummary(PrincipalDetails principalDetails,
                                                      ContractSummaryRequest request) {
        AccessScope accessScope = accessScopeResolver.resolve(principalDetails);
        String organizationCode = normalizeNullable(request.getOrganizationCode());
        validateOrganizationCodeFilter(accessScope, organizationCode);

        YearMonth closingMonth = resolveClosingMonth(request.getClosingMonth());
        LocalDate referenceDate = LocalDate.now();
        LocalDate dueDateLimit = referenceDate.plusDays(30);

        return contractRepository.summarizeHoldingContracts(
                accessScope,
                organizationCode,
                request.getInsuranceCompanyId(),
                closingMonth.toString(),
                referenceDate,
                dueDateLimit
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContractListItemResponse> getContracts(PrincipalDetails principalDetails,
                                                               ContractListRequest request) {
        AccessScope accessScope = accessScopeResolver.resolve(principalDetails);
        String organizationCode = normalizeNullable(request.getOrganizationCode());
        validateOrganizationCodeFilter(accessScope, organizationCode);

        YearMonth closingMonth = resolveClosingMonth(request.getClosingMonth());
        LocalDate referenceDate = LocalDate.now();
        LocalDate dueDateLimit = referenceDate.plusDays(30);

        return PageResponse.from(contractRepository.searchHoldingContracts(
                accessScope,
                organizationCode,
                request.getInsuranceCompanyId(),
                closingMonth.toString(),
                request.getContractStatus(),
                request.getSort(),
                referenceDate,
                dueDateLimit,
                request.toPageable()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceCompanyContractStatusResponse> getInsuranceCompanyContractStatuses(
            PrincipalDetails principalDetails,
            ContractSummaryRequest request
    ) {
        AccessScope accessScope = accessScopeResolver.resolve(principalDetails);
        String organizationCode = normalizeNullable(request.getOrganizationCode());
        validateOrganizationCodeFilter(accessScope, organizationCode);

        YearMonth closingMonth = resolveClosingMonth(request.getClosingMonth());

        return contractRepository.summarizeInsuranceCompanyContractStatuses(
                accessScope,
                organizationCode,
                request.getInsuranceCompanyId(),
                closingMonth.toString()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ContractDetailResponse getContractDetail(PrincipalDetails principalDetails,
                                                    UUID contractId) {
        AccessScope accessScope = accessScopeResolver.resolve(principalDetails);

        ContractDetailQueryResult queryResult = contractRepository.findContractDetail(accessScope, contractId)
                .orElseThrow(() -> {
                    if (contractRepository.existsById(contractId)) {
                        return new BusinessException(AuthErrorCode.USER_FORBIDDEN, "해당 계약을 조회할 권한이 없습니다.");
                    }

                    return new BusinessException(ContractErrorCode.CONTRACT_NOT_FOUND);
                });

        return toContractDetailResponse(queryResult);
    }

    private YearMonth resolveClosingMonth(String closingMonth) {
        String normalizedClosingMonth = normalizeNullable(closingMonth);
        if (normalizedClosingMonth == null) {
            return YearMonth.now().minusMonths(1);
        }
        return YearMonth.parse(normalizedClosingMonth);
    }

    private void validateOrganizationCodeFilter(AccessScope accessScope, String organizationCode) {
        if (organizationCode == null) {
            return;
        }

        if (!accessScope.isAllScope()) {
            throw new BusinessException(AuthErrorCode.USER_FORBIDDEN, "조직 코드로 계약을 조회할 수 있는 권한이 없습니다.");
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ContractDetailResponse toContractDetailResponse(ContractDetailQueryResult queryResult) {
        String customerStatus = toCustomerStatusLabel(queryResult.customerStatus());
        String customerGender = toCustomerGenderLabel(queryResult.customerGender());
        String paymentCycle = toPaymentCycleLabel(queryResult.paymentCycle());

        return ContractDetailResponse.builder()
                .contractSummary(ContractDetailResponse.ContractSummary.builder()
                        .customerName(queryResult.customerName())
                        .customerStatus(customerStatus)
                        .insuranceCompanyName(queryResult.insuranceCompanyName())
                        .insuranceProductName(queryResult.insuranceProductName())
                        .contractStartDate(queryResult.contractStartDate())
                        .contractEndDate(queryResult.contractEndDate())
                        .paymentPeriodYears(queryResult.paymentPeriodYears())
                        .paymentCycle(paymentCycle)
                        .build())
                .customerInfo(ContractDetailResponse.CustomerInfo.builder()
                        .customerName(queryResult.customerName())
                        .customerGender(customerGender)
                        .customerBirthDate(queryResult.customerBirthDate())
                        .customerPhone(queryResult.customerPhone())
                        .customerEmail(queryResult.customerEmail())
                        .customerAddress(queryResult.customerAddress())
                        .build())
                .contractInfo(ContractDetailResponse.ContractInfo.builder()
                        .insuranceCompanyName(queryResult.insuranceCompanyName())
                        .insuranceProductName(queryResult.insuranceProductName())
                        .contractStartDate(queryResult.contractStartDate())
                        .contractEndDate(queryResult.contractEndDate())
                        .coverageStartDate(queryResult.coverageStartDate())
                        .coverageEndDate(queryResult.coverageEndDate())
                        .paymentPeriodYears(queryResult.paymentPeriodYears())
                        .paymentCycle(paymentCycle)
                        .build())
                .build();
    }

    private String toCustomerStatusLabel(String customerStatus) {
        if ("CONTRACTED".equals(customerStatus)) {
            return "정식 고객";
        }

        if ("PROSPECT".equals(customerStatus)) {
            return "잠재 고객";
        }

        return customerStatus;
    }

    private String toCustomerGenderLabel(String customerGender) {
        if ("FEMALE".equals(customerGender)) {
            return "여";
        }

        if ("MALE".equals(customerGender)) {
            return "남";
        }

        return customerGender;
    }

    private String toPaymentCycleLabel(String paymentCycle) {
        if ("MONTHLY".equals(paymentCycle)) {
            return "월납";
        }

        return paymentCycle;
    }
}
