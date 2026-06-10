package com.linker.relia.customer.service;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.security.principal.PrincipalDetails;

import java.util.UUID;

public interface CustomerAccessService {
    AccessScope resolveAccessScope(PrincipalDetails principalDetails);

    void validateOrganizationCodeFilter(AccessScope accessScope, String organizationCode);

    void validateCustomerAccess(AccessScope accessScope, UUID customerId);
}
