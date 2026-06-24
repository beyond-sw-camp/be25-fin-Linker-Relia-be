package com.linker.relia.customer.service;

import com.linker.relia.auth.exception.AuthErrorCode;
import com.linker.relia.common.access.AccessScope;
import com.linker.relia.common.access.AccessScopeResolver;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.customer.exception.CustomerErrorCode;
import com.linker.relia.customer.repository.CustomerRepository;
import com.linker.relia.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerAccessServiceImpl implements CustomerAccessService {
    private final AccessScopeResolver accessScopeResolver;
    private final CustomerRepository customerRepository;

    @Override
    public AccessScope resolveAccessScope(PrincipalDetails principalDetails) {
        return accessScopeResolver.resolve(principalDetails);
    }

    @Override
    public void validateOrganizationCodeFilter(AccessScope accessScope, String organizationCode) {
        if (organizationCode == null) {
            return;
        }

        if (!accessScope.isAllScope()) {
            throw new BusinessException(AuthErrorCode.USER_FORBIDDEN, "조직 코드로 고객을 조회할 수 있는 권한이 없습니다.");
        }
    }
    @Override
    public void validateCustomerAccess(AccessScope accessScope, UUID customerId) {
        if (customerRepository.existsAccessibleCustomer(accessScope, customerId)) {
            return;
        }

        if (customerRepository.existsByIdAndDeletedAtIsNull(customerId)) {
            throw new BusinessException(AuthErrorCode.USER_FORBIDDEN, "해당 고객에 접근할 권한이 없습니다.");
        }

        throw new BusinessException(CustomerErrorCode.CUSTOMER_NOT_FOUND);
    }
}
