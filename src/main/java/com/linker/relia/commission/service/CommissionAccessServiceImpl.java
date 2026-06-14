package com.linker.relia.commission.service;

import com.linker.relia.auth.exception.AuthErrorCode;
import com.linker.relia.common.access.AccessScope;
import com.linker.relia.common.access.AccessScopeResolver;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.organization.domain.Organization;
import com.linker.relia.organization.exception.OrganizationErrorCode;
import com.linker.relia.organization.repository.OrganizationRepository;
import com.linker.relia.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommissionAccessServiceImpl implements CommissionAccessService {
    private final AccessScopeResolver accessScopeResolver;
    private final OrganizationRepository organizationRepository;

    @Override
    public AccessScope resolveAccessScope(PrincipalDetails principalDetails) {
        return accessScopeResolver.resolve(principalDetails);
    }

    @Override
    public void validateOrganizationCodeFilter(AccessScope accessScope, String organizationCode, String userOrganizationCode) {
        if (organizationCode == null) {
            return;
        }

        if (accessScope.isAllScope()) {
            return;
        }

        if (!organizationCode.equals(userOrganizationCode)) {
            throw new BusinessException(AuthErrorCode.USER_FORBIDDEN, "다른 지점의 수수료 정보는 조회할 수 없습니다.");
        }
    }

    @Override
    public Organization resolveOrganization(String organizationCode) {
        return organizationRepository.findByOrganizationCode(organizationCode)
                .filter(found -> found.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));
    }
}
