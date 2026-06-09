package com.linker.relia.organization.repository;

import com.linker.relia.organization.domain.Organization;
import com.linker.relia.organization.domain.OrganizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    Optional<Organization> findByOrganizationCode(String organizationCode);

    List<Organization> findAllByDeletedAtIsNullOrderByCreatedAtAsc();

    List<Organization> findAllByOrganizationStatusAndDeletedAtIsNullOrderByCreatedAtAsc(OrganizationStatus organizationStatus);
}
