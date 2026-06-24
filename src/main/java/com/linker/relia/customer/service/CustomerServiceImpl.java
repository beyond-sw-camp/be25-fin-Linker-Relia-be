package com.linker.relia.customer.service;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.consultation.dto.request.ConsultationHistoryRequest;
import com.linker.relia.consultation.dto.response.ConsultationHistoryItemResponse;
import com.linker.relia.consultation.repository.ConsultationRepository;
import com.linker.relia.contract.repository.ContractRepository;
import com.linker.relia.customer.dto.CustomerContractSummaryResponse;
import com.linker.relia.customer.dto.CustomerDetailQueryResult;
import com.linker.relia.customer.dto.CustomerDetailResponse;
import com.linker.relia.customer.dto.CustomerFpHistoryItemResponse;
import com.linker.relia.customer.dto.CustomerFpHistoryRequest;
import com.linker.relia.customer.dto.CustomerInterestItemResponse;
import com.linker.relia.customer.dto.CustomerInterestListRequest;
import com.linker.relia.customer.dto.CustomerInterestListResponse;
import com.linker.relia.customer.dto.CustomerInterestSummaryResponse;
import com.linker.relia.customer.dto.CustomerListItemResponse;
import com.linker.relia.customer.dto.CustomerListRequest;
import com.linker.relia.customer.dto.CustomerListResponse;
import com.linker.relia.customer.dto.CustomerListSummaryResponse;
import com.linker.relia.customer.dto.CustomerOwnedContractRequest;
import com.linker.relia.customer.dto.CustomerOwnedContractResponse;
import com.linker.relia.customer.exception.CustomerErrorCode;
import com.linker.relia.customer.repository.CustomerFpHistoryRepository;
import com.linker.relia.customer.repository.CustomerRepository;
import com.linker.relia.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;
    private final CustomerFpHistoryRepository customerFpHistoryRepository;
    private final ContractRepository contractRepository;
    private final ConsultationRepository consultationRepository;
    private final CustomerAccessService customerAccessService;

    @Override
    @Transactional(readOnly = true)
    public CustomerListResponse getCustomers(PrincipalDetails principalDetails, CustomerListRequest request) {
        AccessScope accessScope = customerAccessService.resolveAccessScope(principalDetails);
        Pageable pageable = request.toPageable();
        String customerName = normalizeNullable(request.getCustomerName());
        String organizationCode = normalizeNullable(request.getOrganizationCode());

        customerAccessService.validateOrganizationCodeFilter(accessScope, organizationCode);

        CustomerListSummaryResponse summary = customerRepository.summarizeCustomers(accessScope, organizationCode);

        Page<CustomerListItemResponse> customerPage = customerRepository.searchCustomers(
                accessScope,
                customerName,
                organizationCode,
                request.getCustomerStatus(),
                pageable
        );

        return CustomerListResponse.builder()
                .summary(summary)
                .customers(PageResponse.from(customerPage))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerInterestListResponse getInterestCustomers(PrincipalDetails principalDetails,
                                                             CustomerInterestListRequest request) {
        AccessScope accessScope = customerAccessService.resolveAccessScope(principalDetails);
        Pageable pageable = request.toPageable();
        String customerName = normalizeNullable(request.getCustomerName());
        String organizationCode = normalizeNullable(request.getOrganizationCode());
        String interestReason = request.getInterestReason() == null ? null : request.getInterestReason().name();

        customerAccessService.validateOrganizationCodeFilter(accessScope, organizationCode);

        CustomerInterestSummaryResponse summary = customerRepository.summarizeInterestCustomers(
                accessScope,
                organizationCode
        );

        Page<CustomerInterestItemResponse> customerPage = customerRepository.searchInterestCustomers(
                accessScope,
                customerName,
                organizationCode,
                interestReason,
                pageable
        );

        return CustomerInterestListResponse.builder()
                .summary(summary)
                .customers(PageResponse.from(customerPage))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDetailResponse getCustomerDetail(PrincipalDetails principalDetails, UUID customerId) {
        AccessScope accessScope = customerAccessService.resolveAccessScope(principalDetails);
        customerAccessService.validateCustomerAccess(accessScope, customerId);

        CustomerDetailQueryResult customerDetail = customerRepository.findCustomerDetail(accessScope, customerId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUSTOMER_NOT_FOUND));

        CustomerContractSummaryResponse contractSummary = contractRepository.summarizeCustomerContracts(customerId);

        return CustomerDetailResponse.builder()
                .customerId(customerDetail.customerId())
                .customerName(customerDetail.customerName())
                .customerStatus(customerDetail.customerStatus())
                .interestYn(customerDetail.interestYn())
                .interestReason(customerDetail.interestReason())
                .customerGrade(customerDetail.customerGrade())
                .customerBirthDate(customerDetail.customerBirthDate())
                .customerGender(customerDetail.customerGender())
                .customerPhone(customerDetail.customerPhone())
                .customerEmail(customerDetail.customerEmail())
                .customerAddress(customerDetail.customerAddress())
                .customerJob(customerDetail.customerJob())
                .customerCompanyName(customerDetail.customerCompanyName())
                .fpId(customerDetail.fpId())
                .fpName(customerDetail.fpName())
                .organizationCode(customerDetail.organizationCode())
                .organizationName(customerDetail.organizationName())
                .lastConsultedAt(toLocalDate(customerDetail.lastConsultedAt()))
                .nextConsultedAt(toLocalDate(customerDetail.nextConsultedAt()))
                .contractEndDate(customerDetail.contractEndDate())
                .unpaidInstallmentCount(customerDetail.unpaidInstallmentCount())
                .renewalDDay(customerDetail.renewalDDay())
                .maturityDDay(customerDetail.maturityDDay())
                .contractSummary(contractSummary == null ? CustomerContractSummaryResponse.empty() : contractSummary)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerOwnedContractResponse> getOwnCustomerContracts(PrincipalDetails principalDetails,
                                                                               UUID customerId,
                                                                               CustomerOwnedContractRequest request) {
        AccessScope accessScope = customerAccessService.resolveAccessScope(principalDetails);
        customerAccessService.validateCustomerAccess(accessScope, customerId);

        Page<CustomerOwnedContractResponse> contractPage = contractRepository.findOwnCustomerContracts(
                customerId,
                request.getContractStatus(),
                request.toPageable()
        );

        return PageResponse.from(contractPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ConsultationHistoryItemResponse> getOwnCustomerConsultations(PrincipalDetails principalDetails,
                                                                                     UUID customerId,
                                                                                     ConsultationHistoryRequest request) {
        AccessScope accessScope = customerAccessService.resolveAccessScope(principalDetails);
        customerAccessService.validateCustomerAccess(accessScope, customerId);

        Page<ConsultationHistoryItemResponse> consultationPage = consultationRepository.findOwnCustomerConsultations(
                accessScope,
                customerId,
                request.toPageable()
        );

        return PageResponse.from(consultationPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerFpHistoryItemResponse> getCustomerFpHistories(PrincipalDetails principalDetails,
                                                                              UUID customerId,
                                                                              CustomerFpHistoryRequest request) {
        AccessScope accessScope = customerAccessService.resolveAccessScope(principalDetails);
        customerAccessService.validateCustomerAccess(accessScope, customerId);

        Page<CustomerFpHistoryItemResponse> historyPage = customerFpHistoryRepository.findCustomerFpHistories(
                customerId,
                request.toPageable()
        );

        return PageResponse.from(historyPage);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate();
    }
}
