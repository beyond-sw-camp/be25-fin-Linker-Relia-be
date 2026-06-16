package com.linker.relia.hr.dto;

import com.linker.relia.hr.domain.HrMonthlyClosing;
import com.linker.relia.user.domain.UserRole;
import com.linker.relia.user.domain.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class HrClosingUserItemResponse {
    private final UUID userId;
    private final String empCode;
    private final String userName;
    private final UserRole userRole;
    private final UserStatus userStatus;
    private final UUID organizationId;
    private final String organizationCode;
    private final String organizationName;
    private final LocalDateTime closedAt;

    public static HrClosingUserItemResponse from(HrMonthlyClosing closing) {
        return HrClosingUserItemResponse.builder()
                .userId(closing.getUser().getId())
                .empCode(closing.getEmpCode())
                .userName(closing.getUserName())
                .userRole(closing.getUserRole())
                .userStatus(closing.getUserStatus())
                .organizationId(closing.getOrganization().getId())
                .organizationCode(closing.getOrganization().getOrganizationCode())
                .organizationName(closing.getOrganization().getOrganizationName())
                .closedAt(closing.getClosedAt())
                .build();
    }
}
