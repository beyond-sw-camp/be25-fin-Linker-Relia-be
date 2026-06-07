package com.linker.relia.customer.policy;

import com.linker.relia.auth.exception.AuthErrorCode;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.security.principal.PrincipalDetails;
import com.linker.relia.user.domain.User;
import com.linker.relia.user.domain.UserRole;
import org.springframework.stereotype.Component;

@Component
public class CustomerAccessPolicy {
    public CustomerAccessScope resolve(PrincipalDetails principalDetails) {
        User user = principalDetails.getUser();
        UserRole userRole = user.getUserRole();

        return switch (userRole) {
            case FP -> new CustomerAccessScope(
                    CustomerAccessScopeType.OWN_CUSTOMERS,
                    user.getId(),
                    user.getOrganization().getId()
            );
            case BRANCH_MANAGER -> new CustomerAccessScope(
                    CustomerAccessScopeType.BRANCH_CUSTOMERS,
                    user.getId(),
                    user.getOrganization().getId()
            );
            case HQ_MANAGER -> new CustomerAccessScope(
                    CustomerAccessScopeType.ALL_CUSTOMERS,
                    user.getId(),
                    user.getOrganization().getId()
            );
            case SYSTEM_ADMIN -> throw new BusinessException(AuthErrorCode.USER_FORBIDDEN);
        };
    }
}
