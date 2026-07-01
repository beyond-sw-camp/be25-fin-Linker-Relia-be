package com.linker.relia.organization.repository;

import com.linker.relia.organization.domain.Organization;
import com.linker.relia.organization.domain.OrganizationStatus;
import com.linker.relia.organization.domain.OrganizationType;
import com.linker.relia.organization.dto.BranchOrganizationResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    Optional<Organization> findByOrganizationCode(String organizationCode);

    List<Organization> findAllByDeletedAtIsNullOrderByCreatedAtAsc();

    List<Organization> findAllByOrganizationStatusAndDeletedAtIsNullOrderByCreatedAtAsc(OrganizationStatus organizationStatus);

    List<Organization> findAllByOrganizationTypeAndOrganizationStatusAndDeletedAtIsNullOrderByCreatedAtAsc(
            OrganizationType organizationType,
            OrganizationStatus organizationStatus
    );

    @Query("""
            select new com.linker.relia.organization.dto.BranchOrganizationResponse(
                organization.id,
                organization.organizationCode,
                organization.organizationName,
                organization.organizationAddress,
                organization.organizationPhone,
                organization.organizationStatus,
                branchManager.userName,
                count(advisor.id)
            )
            from Organization organization
            left join User branchManager
                on branchManager.organization = organization
                and branchManager.userRole = com.linker.relia.user.domain.UserRole.BRANCH_MANAGER
                and branchManager.deletedAt is null
            left join User advisor
                on advisor.organization = organization
                and advisor.userRole = com.linker.relia.user.domain.UserRole.FP
                and advisor.userStatus = com.linker.relia.user.domain.UserStatus.ACTIVE
                and advisor.deletedAt is null
            where organization.organizationType = com.linker.relia.organization.domain.OrganizationType.BRANCH
                and organization.deletedAt is null
            group by
                organization.id,
                organization.organizationCode,
                organization.organizationName,
                organization.organizationAddress,
                organization.organizationPhone,
                organization.organizationStatus,
                branchManager.userName,
                organization.createdAt
            order by organization.createdAt asc
            """)
    List<BranchOrganizationResponse> findBranchOrganizationsWithAdvisorCount();
}
