package com.linker.relia.dashboard.repository;

import com.linker.relia.dashboard.dto.DashboardBranchRankingItemResponse;
import com.linker.relia.dashboard.dto.DashboardRankOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface DashboardBranchRankingQueryRepository {

    Page<DashboardBranchRankingItemResponse> findBranchRankings(
            String closingMonth,
            String comparisonClosingMonth,
            DashboardRankOrder rankOrder,
            Pageable pageable
    );

    Optional<String> findLatestClosingMonth();
}
