package com.linker.relia.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
public class FpMonthlyPerformanceResponse {
    private final UUID fpId;
    private final List<FpMonthlyPerformanceItemResponse> performances;
}
