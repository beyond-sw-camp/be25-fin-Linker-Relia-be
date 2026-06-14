package com.linker.relia.organization.repository;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.organization.dto.FpListItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrganizationFpRepository {
    Page<FpListItemResponse> searchFps(AccessScope accessScope,
                                       String keyword,
                                       UUID organizationId,
                                       String closingMonth,
                                       Pageable pageable);
}
