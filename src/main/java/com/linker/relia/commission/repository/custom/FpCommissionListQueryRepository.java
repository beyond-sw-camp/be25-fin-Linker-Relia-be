package com.linker.relia.commission.repository.custom;

import com.linker.relia.commission.dto.FpCommissionListQueryResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FpCommissionListQueryRepository {
    Page<FpCommissionListQueryResult> findBranchFpCommissionList(String closingMonth, UUID organizationId, Pageable pageable);

    Page<FpCommissionListQueryResult> findHqFpCommissionList(String closingMonth, Pageable pageable);
}
