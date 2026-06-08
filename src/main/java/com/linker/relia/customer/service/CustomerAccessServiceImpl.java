package com.linker.relia.customer.service;

import com.linker.relia.auth.exception.AuthErrorCode;
import com.linker.relia.common.access.AccessScope;
import com.linker.relia.common.access.AccessScopeResolver;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.common.exception.CommonErrorCode;
import com.linker.relia.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerAccessServiceImpl implements CustomerAccessService {
    private final AccessScopeResolver accessScopeResolver;

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
}
