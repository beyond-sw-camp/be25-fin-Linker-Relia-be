package com.linker.relia.organization.dto;

import com.linker.relia.common.dto.request.PageQueryRequest;
import com.linker.relia.user.domain.UserRole;
import com.linker.relia.user.domain.UserStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class OrganizationMemberListRequest extends PageQueryRequest {
    private String keyword;
    private String branchKeyword;
    private UUID organizationId;
    private UserRole role;
    private UserStatus status;
    private OrganizationMemberSort sort = OrganizationMemberSort.NAME_ASC;
}
