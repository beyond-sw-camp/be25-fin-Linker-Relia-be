package com.linker.relia.organization.dto;

import com.linker.relia.user.domain.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
public class FpListItemResponse {
    private final UUID id;
    private final String empCode;
    private final String userName;
    private final UUID organizationId;
    private final String organizationName;
    private final String closingMonth;
    private final Integer rank;
    private final long customerCount;
    private final long contractCount;
    private final BigDecimal retentionRate;
    private final Integer totalRank;
    private final Integer branchRank;
    private final BigDecimal performanceScore;
    private final UserStatus userStatus;
    private final LocalDate resignedAt;
}
