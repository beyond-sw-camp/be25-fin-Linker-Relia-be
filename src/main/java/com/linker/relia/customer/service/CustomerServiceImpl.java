package com.linker.relia.customer.service;

import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.common.exception.CommonErrorCode;
import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.customer.dto.CustomerListItemResponse;
import com.linker.relia.customer.dto.CustomerListRequest;
import com.linker.relia.customer.dto.CustomerListResponse;
import com.linker.relia.customer.dto.CustomerListSummaryResponse;
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

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;
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
                customerName,
                organizationCode,
                request.getCustomerStatus()
        );
        Page<CustomerListItemResponse> customerPage = customerRepository.searchCustomers(
                accessScope.scopeType(),
                accessScope.userId(),
                accessScope.organizationId(),
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
}
