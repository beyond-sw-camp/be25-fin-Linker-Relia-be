package com.linker.relia.common.access;

import com.linker.relia.security.principal.PrincipalDetails;
import com.linker.relia.user.domain.User;
import com.linker.relia.user.domain.UserRole;
import org.springframework.stereotype.Component;

@Component
public class AccessScopeResolver {
    public AccessScope resolve(PrincipalDetails principalDetails) {
        User user = principalDetails.getUser();
        UserRole userRole = user.getUserRole();

        return switch (userRole) {
            case FP -> new AccessScope(
                    AccessScopeType.OWN,
                    user.getId(),
                    user.getOrganization().getId()
            );
            case BRANCH_MANAGER -> new AccessScope(
                    AccessScopeType.BRANCH,
                    user.getId(),
                    user.getOrganization().getId()
            );
            case HQ_MANAGER, SYSTEM_ADMIN -> new AccessScope(
                    AccessScopeType.ALL,
                    user.getId(),
                    user.getOrganization().getId()
            );
        };
    }
}
