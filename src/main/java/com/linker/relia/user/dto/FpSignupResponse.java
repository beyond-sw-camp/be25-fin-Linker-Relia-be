package com.linker.relia.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FpSignupResponse {
    private final String userId;
    private final String empCode;
    private final String loginId;
    private final String userName;
    private final String email;
    private final String organizationCode;
}
