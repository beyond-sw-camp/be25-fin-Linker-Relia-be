package com.linker.relia.commission.service;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.organization.domain.Organization;
import com.linker.relia.security.principal.PrincipalDetails;

public interface CommissionAccessService {
    AccessScope resolveAccessScope(PrincipalDetails principalDetails);

    void validateOrganizationCodeFilter(AccessScope accessScope,
                                        String organizationCode,
                                        String userOrganizationCode);

    Organization resolveOrganization(String organizationCode);
}
