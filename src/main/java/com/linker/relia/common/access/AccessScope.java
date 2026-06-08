package com.linker.relia.common.access;

import java.util.UUID;

public record AccessScope(
        AccessScopeType scopeType,
        UUID userId,
        UUID organizationId
) {
    public boolean isOwnScope() {
        return scopeType == AccessScopeType.OWN;
    }

    public boolean isBranchScope() {
        return scopeType == AccessScopeType.BRANCH;
    }

    public boolean isAllScope() {
        return scopeType == AccessScopeType.ALL;
    }
}
