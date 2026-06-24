package com.linker.relia.commission.repository.custom;

import com.linker.relia.commission.dto.FpCommissionMonthlyTrendQueryResult;

import java.util.List;
import java.util.UUID;

public interface FpCommissionTrendQueryRepository {
    List<FpCommissionMonthlyTrendQueryResult> findFpTrendQueryResults(String startMonth,
                                                                      String endMonth,
                                                                      UUID fpId);
}
