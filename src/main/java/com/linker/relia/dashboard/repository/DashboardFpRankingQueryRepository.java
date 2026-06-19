package com.linker.relia.dashboard.repository;

import com.linker.relia.dashboard.dto.DashboardFpRankingItemResponse;
import com.linker.relia.dashboard.dto.DashboardFpRankOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface DashboardFpRankingQueryRepository {

    Page<DashboardFpRankingItemResponse> findFpRankings(
            String closingMonth,
            UUID organizationId,
            DashboardFpRankOrder rankOrder,
            Pageable pageable
    );

    Optional<String> findLatestClosingMonth();
}
