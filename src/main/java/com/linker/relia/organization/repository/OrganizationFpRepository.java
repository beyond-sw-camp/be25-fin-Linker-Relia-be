package com.linker.relia.organization.repository;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.organization.dto.FpContractListItemResponse;
import com.linker.relia.organization.dto.FpDetailResponse;
import com.linker.relia.organization.dto.FpListItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationFpRepository {
    Page<FpListItemResponse> searchFps(AccessScope accessScope,
                                       String keyword,
                                       UUID organizationId,
                                       String closingMonth,
                                       Pageable pageable);

    Optional<FpDetailResponse> findFpDetail(AccessScope accessScope,
                                            UUID fpId,
                                            String closingMonth);

    boolean existsFp(UUID fpId);

    boolean existsFpInScope(AccessScope accessScope, UUID fpId);

    Page<FpContractListItemResponse> findFpContracts(AccessScope accessScope,
                                                     UUID fpId,
                                                     Pageable pageable);
}
