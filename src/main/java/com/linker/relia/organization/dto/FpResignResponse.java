package com.linker.relia.organization.dto;

import com.linker.relia.user.domain.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class FpResignResponse {
    private final UUID id;
    private final UserStatus userStatus;
    private final LocalDate resignedAt;
    private final int handoverRequestCount;
}
