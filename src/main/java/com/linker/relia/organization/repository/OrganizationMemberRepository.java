package com.linker.relia.organization.repository;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.organization.dto.OrganizationMemberItemResponse;
import com.linker.relia.organization.dto.OrganizationMemberSort;
import com.linker.relia.user.domain.UserRole;
import com.linker.relia.user.domain.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrganizationMemberRepository {
    Page<OrganizationMemberItemResponse> searchMembers(AccessScope accessScope,
                                                       String keyword,
                                                       String branchKeyword,
                                                       UUID organizationId,
                                                       UserRole role,
                                                       UserStatus status,
                                                       OrganizationMemberSort sort,
                                                       Pageable pageable);
}
