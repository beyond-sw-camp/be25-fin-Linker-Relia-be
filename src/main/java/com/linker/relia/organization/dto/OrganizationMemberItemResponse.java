package com.linker.relia.organization.dto;

import com.linker.relia.user.domain.UserRole;
import com.linker.relia.user.domain.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class OrganizationMemberItemResponse {
    private final UUID id;
    private final UUID fpId;
    private final String empCode;
    private final String userName;
    private final UUID organizationId;
    private final String organizationName;
    private final UserRole userRole;
    private final String email;
    private final String phone;
    private final UserStatus userStatus;
}
