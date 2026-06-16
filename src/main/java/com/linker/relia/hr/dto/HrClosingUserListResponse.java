package com.linker.relia.hr.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HrClosingUserListResponse {
    private final String closingMonth;
    private final int count;
    private final List<HrClosingUserItemResponse> users;

    public static HrClosingUserListResponse of(String closingMonth, List<HrClosingUserItemResponse> users) {
        return HrClosingUserListResponse.builder()
                .closingMonth(closingMonth)
                .count(users.size())
                .users(users)
                .build();
    }
}
