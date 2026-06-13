package com.linker.relia.organization.repository;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.organization.dto.FpListItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrganizationFpRepository {
    /**
                                        * Searches financial partners (FPs) within an organization using access scope, a keyword, closing-month filter and pagination.
                                        *
                                        * @param accessScope  the access/authorization context that constrains which FPs are visible
                                        * @param keyword      free-text term to filter FP results (may be null or empty to disable keyword filtering)
                                        * @param organizationId  UUID of the organization whose FPs are being searched
                                        * @param closingMonth  closing month filter in string form (format expected by implementation; may be null to disable)
                                        * @param pageable     pagination and sorting instructions
                                        * @return             a page of FpListItemResponse objects matching the provided criteria
                                        */
                                       Page<FpListItemResponse> searchFps(AccessScope accessScope,
                                       String keyword,
                                       UUID organizationId,
                                       String closingMonth,
                                       Pageable pageable);
}
