package com.linker.relia.contract.service;

import com.linker.relia.auth.exception.AuthErrorCode;
import com.linker.relia.common.access.AccessScope;
import com.linker.relia.common.access.AccessScopeResolver;
import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.contract.domain.Contract;
import com.linker.relia.contract.dto.ContractCreateRequest;
import com.linker.relia.contract.dto.ContractCreateResponse;
import com.linker.relia.contract.dto.ContractDetailQueryResult;
import com.linker.relia.contract.dto.ContractDetailResponse;
import com.linker.relia.contract.dto.ContractListItemResponse;
import com.linker.relia.contract.dto.ContractListRequest;
import com.linker.relia.contract.dto.ContractMonthlyTrendResponse;
import com.linker.relia.contract.dto.ContractSummaryRequest;
import com.linker.relia.contract.dto.ContractSummaryResponse;
import com.linker.relia.contract.dto.InsuranceCompanyContractStatusResponse;
import com.linker.relia.contract.exception.ContractErrorCode;
import com.linker.relia.contract.repository.ContractRepository;
import com.linker.relia.customer.domain.Customer;
import com.linker.relia.customer.exception.CustomerErrorCode;
import com.linker.relia.customer.repository.CustomerRepository;
import com.linker.relia.insurance.domain.InsuranceProduct;
import com.linker.relia.insurance.repository.InsuranceProductRepository;
import com.linker.relia.security.principal.PrincipalDetails;
import com.linker.relia.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String DEFAULT_CONTRACT_STATUS = "MAINTENANCE";
    private static final String LAPSED_CONTRACT_STATUS = "LAPSED";
    private static final String MONTHLY_PAYMENT_CYCLE = "MONTHLY";
    private static final String CONTRACT_CODE_PREFIX = "CTR";
    private static final int CONTRACT_CODE_GENERATION_MAX_RETRY = 3;
    private static final Collection<String> DUPLICATE_BLOCKING_CONTRACT_STATUSES = List.of(
            DEFAULT_CONTRACT_STATUS,
            LAPSED_CONTRACT_STATUS
    );

    private final ContractRepository contractRepository;
    private final CustomerRepository customerRepository;
    private final InsuranceProductRepository insuranceProductRepository;
    private final AccessScopeResolver accessScopeResolver;
    private final PlatformTransactionManager transactionManager;

    @Override
    public synchronized ContractCreateResponse createContract(PrincipalDetails principalDetails,
                                                              ContractCreateRequest request) {
        for (int attempt = 0; attempt < CONTRACT_CODE_GENERATION_MAX_RETRY; attempt++) {
            try {
                TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
                return transactionTemplate.execute(status -> createContractInTransaction(principalDetails, request));
            } catch (DataIntegrityViolationException exception) {
                if (attempt == CONTRACT_CODE_GENERATION_MAX_RETRY - 1) {
                    throw new BusinessException(ContractErrorCode.DUPLICATE_CONTRACT_CODE);
                }
            }
        }

        throw new BusinessException(ContractErrorCode.DUPLICATE_CONTRACT_CODE);
    }

    private ContractCreateResponse createContractInTransaction(PrincipalDetails principalDetails,
                                                              ContractCreateRequest request) {
        User fp = principalDetails.getUser();
        Customer customer = customerRepository.findByIdAndDeletedAtIsNull(request.getCustomerId())
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUSTOMER_NOT_FOUND));
        validateCustomerOwner(fp, customer);

        InsuranceProduct insuranceProduct = insuranceProductRepository
                .findByIdAndInsuranceProductStatusAndDeletedAtIsNull(
                        request.getInsuranceProductId(),
                        ACTIVE_STATUS
                )
                .orElseThrow(() -> new BusinessException(ContractErrorCode.INSURANCE_PRODUCT_NOT_FOUND));

        validateDuplicateContract(customer, insuranceProduct);

        String contractCode = generateContractCode();
        validateContractDates(request);
        validatePaymentCycle(request.getPaymentCycle());

        LocalDate coverageStartDate = request.getCoverageStartDate() == null
                ? request.getContractStartDate()
                : request.getCoverageStartDate();
        LocalDate coverageEndDate = request.getCoverageEndDate() == null
                ? request.getContractEndDate()
                : request.getCoverageEndDate();
        validateCoverageDates(coverageStartDate, coverageEndDate);

        Contract contract = Contract.builder()
                .id(UUID.randomUUID())
                .contractCode(contractCode)
                .customer(customer)
                .fp(fp)
                .insuranceProduct(insuranceProduct)
                .contractDate(request.getContractDate())
                .contractStartDate(request.getContractStartDate())
                .contractEndDate(request.getContractEndDate())
                .contractStatus(DEFAULT_CONTRACT_STATUS)
                .paymentPeriodYears(request.getPaymentPeriodYears())
                .paymentCycle(MONTHLY_PAYMENT_CYCLE)
                .monthlyPremium(request.getMonthlyPremium())
                .coverageStartDate(coverageStartDate)
                .coverageEndDate(coverageEndDate)
                .coverageSummary(normalizeNullable(request.getCoverageSummary()))
                .build();

        Contract savedContract = contractRepository.saveAndFlush(contract);
        customer.markAsContracted();

        return ContractCreateResponse.from(savedContract);
    }

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
    public List<ContractMonthlyTrendResponse> getMonthlyContractTrend(PrincipalDetails principalDetails,
                                                                      ContractSummaryRequest request) {
        AccessScope accessScope = accessScopeResolver.resolve(principalDetails);
        String organizationCode = normalizeNullable(request.getOrganizationCode());
        validateOrganizationCodeFilter(accessScope, organizationCode);

        YearMonth endMonth = resolveClosingMonth(request.getClosingMonth());
        YearMonth startMonth = endMonth.minusMonths(5);

        List<ContractMonthlyTrendResponse> monthlyTrend = contractRepository.summarizeMonthlyContractTrend(
                accessScope,
                organizationCode,
                request.getInsuranceCompanyId(),
                startMonth.toString(),
                endMonth.toString()
        );

        Map<String, ContractMonthlyTrendResponse> trendByMonth = new HashMap<>();
        for (ContractMonthlyTrendResponse trend : monthlyTrend) {
            trendByMonth.put(trend.getMonth(), trend);
        }

        List<ContractMonthlyTrendResponse> result = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            YearMonth month = startMonth.plusMonths(i);
            String monthValue = month.toString();
            result.add(trendByMonth.getOrDefault(monthValue, emptyMonthlyTrend(monthValue)));
        }

        return result;
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

    private void validateCustomerOwner(User fp, Customer customer) {
        if (customer.getCustomerFp() == null || !fp.getId().equals(customer.getCustomerFp().getId())) {
            throw new BusinessException(AuthErrorCode.USER_FORBIDDEN, "담당 고객에 대해서만 계약을 등록할 수 있습니다.");
        }
    }

    private String generateContractCode() {
        long nextSequence = contractRepository.findMaxContractCodeSequence() + 1;
        return CONTRACT_CODE_PREFIX + String.format("%06d", nextSequence);
    }

    private void validateDuplicateContract(Customer customer, InsuranceProduct insuranceProduct) {
        boolean exists = contractRepository.existsByCustomerAndInsuranceProductAndContractStatusInAndDeletedAtIsNull(
                customer,
                insuranceProduct,
                DUPLICATE_BLOCKING_CONTRACT_STATUSES
        );

        if (exists) {
            throw new BusinessException(ContractErrorCode.DUPLICATE_ACTIVE_OR_LAPSED_CONTRACT);
        }
    }

    private void validateContractDates(ContractCreateRequest request) {
        if (request.getContractStartDate().isAfter(request.getContractEndDate())) {
            throw new BusinessException(ContractErrorCode.INVALID_CONTRACT_DATE);
        }
    }

    private void validateCoverageDates(LocalDate coverageStartDate, LocalDate coverageEndDate) {
        if (coverageStartDate.isAfter(coverageEndDate)) {
            throw new BusinessException(ContractErrorCode.INVALID_CONTRACT_DATE);
        }
    }

    private void validatePaymentCycle(String paymentCycle) {
        if (!MONTHLY_PAYMENT_CYCLE.equals(paymentCycle)) {
            throw new BusinessException(ContractErrorCode.INVALID_PAYMENT_CYCLE);
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ContractMonthlyTrendResponse emptyMonthlyTrend(String month) {
        return ContractMonthlyTrendResponse.builder()
                .month(month)
                .contractCount(0L)
                .totalMonthlyPremiumAmount(BigDecimal.ZERO)
                .build();
    }

    private ContractDetailResponse toContractDetailResponse(ContractDetailQueryResult queryResult) {
        String customerStatus = toCustomerStatusLabel(queryResult.customerStatus());
        String customerGender = toCustomerGenderLabel(queryResult.customerGender());
        String contractStatus = toContractStatusLabel(queryResult.contractStatus());
        String paymentCycle = toPaymentCycleLabel(queryResult.paymentCycle());

        return ContractDetailResponse.builder()
                .contractSummary(ContractDetailResponse.ContractSummary.builder()
                        .contractCode(queryResult.contractCode())
                        .customerName(queryResult.customerName())
                        .customerStatus(customerStatus)
                        .insuranceCompanyName(queryResult.insuranceCompanyName())
                        .insuranceProductName(queryResult.insuranceProductName())
                        .contractStatus(contractStatus)
                        .monthlyPremium(queryResult.monthlyPremium())
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
                        .customerJob(queryResult.customerJob())
                        .customerCompanyName(queryResult.customerCompanyName())
                        .build())
                .contractInfo(ContractDetailResponse.ContractInfo.builder()
                        .insuranceCompanyName(queryResult.insuranceCompanyName())
                        .insuranceCategoryName(queryResult.insuranceCategoryName())
                        .insuranceProductName(queryResult.insuranceProductName())
                        .contractDate(queryResult.contractDate())
                        .contractStartDate(queryResult.contractStartDate())
                        .contractEndDate(queryResult.contractEndDate())
                        .coverageStartDate(queryResult.coverageStartDate())
                        .coverageEndDate(queryResult.coverageEndDate())
                        .paymentPeriodYears(queryResult.paymentPeriodYears())
                        .paymentCycle(paymentCycle)
                        .monthlyPremium(queryResult.monthlyPremium())
                        .fpName(queryResult.fpName())
                        .fpOrganizationName(queryResult.fpOrganizationName())
                        .createdAt(queryResult.createdAt())
                        .build())
                .coverageInfo(ContractDetailResponse.CoverageInfo.builder()
                        .coverageSummary(queryResult.coverageSummary())
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

    private String toContractStatusLabel(String contractStatus) {
        if ("MAINTENANCE".equals(contractStatus)) {
            return "유지";
        }

        if ("LAPSED".equals(contractStatus)) {
            return "실효";
        }

        if ("COMPLETED".equals(contractStatus)) {
            return "만기";
        }

        if ("TERMINATED".equals(contractStatus)) {
            return "해지";
        }

        return contractStatus;
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
