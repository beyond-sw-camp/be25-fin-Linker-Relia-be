package com.linker.relia.customer.policy;

import java.util.UUID;

public record CustomerAccessScope(
        CustomerAccessScopeType scopeType,
        UUID userId,
        UUID organizationId
) {
}
