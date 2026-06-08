package com.linker.relia.customer.service;

import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.common.exception.CommonErrorCode;
import com.linker.relia.contract.repository.ContractRepository;
import com.linker.relia.customer.dto.CustomerContractSummaryResponse;
import com.linker.relia.customer.dto.CustomerDetailQueryResult;
import com.linker.relia.customer.dto.CustomerDetailResponse;
import com.linker.relia.customer.dto.CustomerListItemResponse;
import com.linker.relia.customer.dto.CustomerListRequest;
import com.linker.relia.customer.dto.CustomerListResponse;
import com.linker.relia.customer.dto.CustomerListSummaryResponse;
import com.linker.relia.customer.exception.CustomerErrorCode;
import com.linker.relia.customer.policy.CustomerAccessPolicy;
import com.linker.relia.customer.policy.CustomerAccessScope;
import com.linker.relia.customer.policy.CustomerAccessScopeType;
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
    private final ContractRepository contractRepository;
    private final CustomerAccessPolicy customerAccessPolicy;

    @Override
    @Transactional(readOnly = true)
    public CustomerListResponse getCustomers(PrincipalDetails principalDetails, CustomerListRequest request) {
        CustomerAccessScope accessScope = customerAccessPolicy.resolve(principalDetails);
        Pageable pageable = request.toPageable();
        String customerName = normalizeNullable(request.getCustomerName());
        String organizationCode = normalizeNullable(request.getOrganizationCode());

        validateOrganizationCodeFilter(accessScope, organizationCode);

        CustomerListSummaryResponse summary = customerRepository.summarizeCustomers(
                accessScope.scopeType(),
                accessScope.userId(),
                accessScope.organizationId(),
                organizationCode
        );

        Page<CustomerListItemResponse> customerPage = customerRepository.searchCustomers(
                accessScope.scopeType(),
                accessScope.userId(),
                accessScope.organizationId(),
                customerName,
                organizationCode,
                request.getCustomerStatus(),
                request.getInterestYn(),
                pageable
        );

        return CustomerListResponse.builder()
                .summary(summary)
                .customers(PageResponse.from(customerPage))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDetailResponse getCustomerDetail(PrincipalDetails principalDetails, UUID customerId) {
        CustomerAccessScope accessScope = customerAccessPolicy.resolve(principalDetails);

        CustomerDetailQueryResult customerDetail = customerRepository.findCustomerDetail(
                        accessScope.scopeType(),
                        accessScope.userId(),
                        accessScope.organizationId(),
                        customerId
                )
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUSTOMER_NOT_FOUND));

        CustomerContractSummaryResponse contractSummary = contractRepository.summarizeCustomerContracts(
                customerId,
                LocalDate.now(),
                LocalDate.now().plusDays(30)
        );

        return CustomerDetailResponse.builder()
                .customerId(customerDetail.customerId())
                .customerName(customerDetail.customerName())
                .customerStatus(customerDetail.customerStatus())
                .interestYn(customerDetail.interestYn())
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
                .contractSummary(contractSummary == null ? CustomerContractSummaryResponse.empty() : contractSummary)
                .build();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateOrganizationCodeFilter(CustomerAccessScope accessScope, String organizationCode) {
        if (organizationCode == null) {
            return;
        }

        if (accessScope.scopeType() != CustomerAccessScopeType.ALL_CUSTOMERS) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "해당 권한에서는 organizationCode를 사용할 수 없습니다.");
        }
    }

    private LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate();
    }
}
